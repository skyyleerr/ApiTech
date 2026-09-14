package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.response.ImportacionResultadoDTO;
import com.apitech.mk5.dto.response.ImportacionResultadoDTO.FilaImportacionDTO;
import com.apitech.mk5.entity.apiario.*;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.evento.ProduccionMiel;
import com.apitech.mk5.repository.apiario.ColmenaRepository;
import com.apitech.mk5.repository.apiario.MedicionRepository;
import com.apitech.mk5.repository.apiario.SensorColmenaRepository;
import com.apitech.mk5.repository.apiario.MonitoreoRepository;
import com.apitech.mk5.repository.evento.ProduccionMielRepository;
import com.apitech.mk5.security.UsuarioContexto;
import com.apitech.mk5.service.ImportacionService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ImportacionServiceImpl implements ImportacionService {
    private static final Set<String> EXTENSIONES = Set.of("csv", "xlsx");
    private static final DateTimeFormatter[] FECHAS = {DateTimeFormatter.ISO_DATE_TIME, DateTimeFormatter.ISO_LOCAL_DATE};
    private final ColmenaRepository colmenas;
    private final SensorColmenaRepository asociaciones;
    private final MedicionRepository mediciones;
    private final ProduccionMielRepository producciones;
    private final MonitoreoRepository monitoreos;
    private final UsuarioContexto contexto;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;

    public ImportacionServiceImpl(ColmenaRepository colmenas, SensorColmenaRepository asociaciones,
                                  MedicionRepository mediciones, ProduccionMielRepository producciones,
                                  MonitoreoRepository monitoreos, UsuarioContexto contexto,
                                  com.apitech.mk5.service.EmpresaOperacionService empresaOperacion) {
        this.colmenas = colmenas; this.asociaciones = asociaciones; this.mediciones = mediciones;
        this.producciones = producciones; this.monitoreos = monitoreos; this.contexto = contexto; this.empresaOperacion = empresaOperacion;
    }

    @Override
    public ImportacionResultadoDTO previsualizar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw new IllegalArgumentException("El archivo está vacío");
        if (archivo.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("El archivo supera el límite de 10 MB");
        String nombre = Optional.ofNullable(archivo.getOriginalFilename()).orElse("");
        String ext = nombre.contains(".") ? nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!EXTENSIONES.contains(ext)) throw new IllegalArgumentException("Solo se aceptan archivos CSV o XLSX");
        try {
            List<String[]> filas = ext.equals("csv") ? leerCsv(archivo) : leerXlsx(archivo);
            if (filas.isEmpty()) throw new IllegalArgumentException("El archivo no contiene registros");
            return validar(filas);
        } catch (IllegalArgumentException e) { throw e; }
          catch (Exception e) { throw new IllegalArgumentException("No se pudo leer el archivo: " + e.getMessage()); }
    }

    private ImportacionResultadoDTO validar(List<String[]> filas) {
        String[] encabezados = filas.get(0);
        Map<String,Integer> col = new HashMap<>();
        for (int i=0;i<encabezados.length;i++) col.put(normalizar(encabezados[i]), i);
        if (!col.containsKey("fecha") || !col.containsKey("idcolmena") && !col.containsKey("colmenaid"))
            throw new IllegalArgumentException("Faltan columnas obligatorias: fecha e id_colmena");
        int cCol = col.containsKey("idcolmena") ? col.get("idcolmena") : col.get("colmenaid");
        Integer cAsoc = col.get("idasociacion"); Integer cTipo = col.get("tipomedicion");
        Integer cValor = col.get("valor"); Integer cUnidad = col.get("unidad"); Integer cOrigen = col.get("origen");
        Integer cProd = col.get("produccion"); Integer cObs = col.get("observaciones"); Integer cFecha = col.get("fecha");
        Integer empresa = contexto.getIdEmpresaActual();
        Set<String> claves = new HashSet<>(); List<FilaImportacionDTO> resultado = new ArrayList<>();
        for (int i=1;i<filas.size();i++) {
            String[] r = filas.get(i); String fecha = valor(r,cFecha); String colmena = valor(r,cCol);
            String asoc = valor(r,cAsoc), tipo = valor(r,cTipo), valor = valor(r,cValor);
            String unidad = valor(r,cUnidad), origen = valor(r,cOrigen), prod = valor(r,cProd), obs = valor(r,cObs);
            String error = null; boolean duplicada = false;
            try {
                if (fecha.isBlank() || colmena.isBlank()) error = "Fecha e id_colmena son obligatorios";
                Integer idCol = parseInt(colmena, "id_colmena");
                Colmena c = error == null ? colmenas.findById(idCol).orElse(null) : null;
                if (error == null && (c == null || empresa != null && !empresa.equals(c.getEmpresa().getIdEmpresa()))) error = "La colmena no existe o no pertenece a tu empresa";
                parseFecha(fecha);
                if (!prod.isBlank()) { float p=parseFloat(prod,"produccion"); if (p < 0) error="La producción no puede ser negativa"; }
                if (!tipo.isBlank() || !valor.isBlank() || !asoc.isBlank()) {
                    if (tipo.isBlank() || valor.isBlank()) error = "Tipo y valor son obligatorios para una medición";
                    TipoMedicion tm = TipoMedicion.valueOf(tipo.toUpperCase(Locale.ROOT));
                    parseFloat(valor,"valor");
                    Integer idA = asoc.isBlank() ? null : parseInt(asoc,"id_asociacion");
                    SensorColmena sc = idA == null ? null : asociaciones.findById(idA).orElse(null);
                    if (idA != null && (sc == null || empresa != null && !empresa.equals(sc.getIdEmpresa()) || !sc.isActivo())) error="La asociación no existe, no pertenece a tu empresa o está inactiva";
                    if (sc != null && sc.getColmena().getIdColmena() != idCol) error="La asociación no corresponde a la colmena indicada";
                    String clave=fecha+"|"+colmena+"|"+asoc+"|"+tipo+"|"+valor;
                    duplicada=!claves.add(clave); if (duplicada && error==null) error="Registro duplicado dentro del archivo";
                }
            } catch (Exception e) { if (error==null) error=e.getMessage(); }
            resultado.add(new FilaImportacionDTO(i+1,fecha,colmena,asoc,tipo,valor,unidad,origen,prod,obs,error,error==null,!duplicada));
        }
        int validas=(int)resultado.stream().filter(FilaImportacionDTO::valida).count();
        int duplicados=(int)resultado.stream().filter(FilaImportacionDTO::duplicada).count();
        return new ImportacionResultadoDTO(resultado, resultado.size(), validas, resultado.size()-validas, duplicados, false);
    }

    @Override @Transactional
    public ImportacionResultadoDTO confirmar(ImportacionResultadoDTO previa) {
        if (previa == null || previa.filas() == null || previa.filas().isEmpty()) throw new IllegalArgumentException("No hay una vista previa para importar");
        if (previa.errores() > 0) throw new IllegalArgumentException("Corrige los errores antes de confirmar la importación");
        Integer empresaId=contexto.getIdEmpresaActual();
        empresaOperacion.validarPuedeOperar(empresaId);
        Empresa empresa = previa.filas().isEmpty()?null:null;
        int guardadas=0;
        for (FilaImportacionDTO f: previa.filas()) {
            Integer idCol=parseInt(f.idColmena(),"id_colmena"); Colmena c=colmenas.findById(idCol).orElseThrow();
            if (empresaId!=null && !empresaId.equals(c.getEmpresa().getIdEmpresa())) throw new IllegalArgumentException("La colmena no pertenece a tu empresa");
            if (!f.tipo().isBlank()) {
                SensorColmena sc = f.idAsociacion().isBlank() ? asociaciones.findByColmena_IdColmenaAndActivoTrue(idCol).stream().filter(a -> a.getSensor().getTipo().name().equalsIgnoreCase(f.tipo())).findFirst().orElseThrow(() -> new IllegalArgumentException("No existe asociación activa para la medición")) : asociaciones.findById(parseInt(f.idAsociacion(),"id_asociacion")).orElseThrow();
                TipoMedicion tipo=TipoMedicion.valueOf(f.tipo().toUpperCase(Locale.ROOT));
                if (sc.getSensor().getTipo() != tipo) throw new IllegalArgumentException("El tipo no coincide con el sensor asociado");
                if (!monitoreos.existsByAsociacion_IdAsociacionAndEstado(sc.getIdAsociacion(), "activo")) {
                    if (!"Estable".equalsIgnoreCase(c.getEstado())) throw new IllegalArgumentException("La colmena debe estar Estable para registrar mediciones");
                    monitoreos.save(new Monitoreo(sc, "activo"));
                }
                OrigenMedicion origen=f.origen().isBlank() ? (sc.getSensor().isEsSimulado()?OrigenMedicion.SIMULADO:OrigenMedicion.REAL) : OrigenMedicion.valueOf(f.origen().toUpperCase(Locale.ROOT));
                if ((sc.getSensor().isEsSimulado() && origen != OrigenMedicion.SIMULADO) || (!sc.getSensor().isEsSimulado() && origen != OrigenMedicion.REAL)) throw new IllegalArgumentException("El origen no coincide con el sensor");
                Medicion m=new Medicion(sc,tipo,parseFloat(f.valor(),"valor"),f.unidad().isBlank()?unidad(tipo):f.unidad(),origen); m.setFecha(parseFecha(f.fecha())); mediciones.save(m); guardadas++;
            }
            if (!f.produccion().isBlank()) { ProduccionMiel p=new ProduccionMiel(c,c.getEmpresa(),parseFloat(f.produccion(),"produccion"),"kg",parseFecha(f.fecha()).toLocalDate(),f.observaciones()); producciones.save(p); guardadas++; }
        }
        return new ImportacionResultadoDTO(previa.filas(), previa.procesadas(), guardadas, 0, previa.duplicados(), true);
    }

    private String unidad(TipoMedicion t){return switch(t){case TEMPERATURA->"°C";case HUMEDAD->"%";case PESO->"kg";};}
    private String valor(String[] r,Integer i){return i==null||i>=r.length||r[i]==null?"":r[i].trim();}
    private String normalizar(String s){return s==null?"":s.replace("\uFEFF","").trim().toLowerCase(Locale.ROOT).replace("_","").replace(" ","");}
    private Integer parseInt(String s,String n){try{return Integer.valueOf(s);}catch(Exception e){throw new IllegalArgumentException(n+" debe ser numérico");}}
    private float parseFloat(String s,String n){try{return Float.parseFloat(s.replace(',','.'));}catch(Exception e){throw new IllegalArgumentException(n+" debe ser numérico");}}
    private LocalDateTime parseFecha(String s){for(DateTimeFormatter f:FECHAS)try{return f==DateTimeFormatter.ISO_LOCAL_DATE?LocalDate.parse(s,f).atStartOfDay():LocalDateTime.parse(s,f);}catch(DateTimeParseException ignored){} throw new IllegalArgumentException("Fecha inválida: "+s);}
    private List<String[]> leerCsv(MultipartFile f)throws Exception{List<String[]> out=new ArrayList<>();try(BufferedReader br=new BufferedReader(new InputStreamReader(f.getInputStream(),StandardCharsets.UTF_8))){String l;while((l=br.readLine())!=null)if(!l.trim().isEmpty())out.add(l.split("[,;]",-1));}return out;}
    private List<String[]> leerXlsx(MultipartFile f)throws Exception{List<String[]> out=new ArrayList<>();try(Workbook w=WorkbookFactory.create(f.getInputStream())){DataFormatter d=new DataFormatter();Sheet s=w.getSheetAt(0);for(Row r:s) {String[] a=new String[r.getLastCellNum()<0?0:r.getLastCellNum()];for(int i=0;i<a.length;i++)a[i]=d.formatCellValue(r.getCell(i));out.add(a);}}return out;}
}
