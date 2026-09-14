package com.apitech.mk5.controller;

import com.apitech.mk5.entity.apiario.Medicion;
import com.apitech.mk5.entity.apiario.TipoMedicion;
import com.apitech.mk5.entity.evento.ProduccionMiel;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.repository.apiario.*;
import com.apitech.mk5.repository.evento.ProduccionMielRepository;
import com.apitech.mk5.security.UsuarioContexto;
import com.apitech.mk5.service.ReporteService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/app/reportes")
@PreAuthorize("hasAnyRole('Admin_Cliente','Empleado_Cliente')")
public class ReporteController {
    private final MedicionRepository mediciones; private final ColmenaRepository colmenas;
    private final SensorColmenaRepository asociaciones; private final ProduccionMielRepository producciones;
    private final AlertaRepository alertas; private final ReporteService reportes; private final UsuarioContexto contexto;
    public ReporteController(MedicionRepository mediciones, ColmenaRepository colmenas, SensorColmenaRepository asociaciones,
                             ProduccionMielRepository producciones, AlertaRepository alertas, ReporteService reportes, UsuarioContexto contexto) {
        this.mediciones=mediciones;this.colmenas=colmenas;this.asociaciones=asociaciones;this.producciones=producciones;this.alertas=alertas;this.reportes=reportes;this.contexto=contexto;
    }
    @GetMapping
    public String ver(@RequestParam(required=false) Integer idAsociacion, @RequestParam(required=false) TipoMedicion tipoMedicion,
                      @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
                      @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
                      @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size, Model model) {
        Integer empresa=contexto.getIdEmpresaActual(); validarAsociacion(idAsociacion,empresa);
        Page<Medicion> resultado=mediciones.buscarReporte(empresa,idAsociacion,tipoMedicion,desde,hasta,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(100,size))));
        LocalDate d=desde==null?null:desde.toLocalDate(), h=hasta==null?null:hasta.toLocalDate();
        List<ProduccionMiel> prod=(d!=null&&h!=null)?producciones.findByEmpresa_IdEmpresaAndFechaBetweenOrderByFechaDesc(empresa,d,h):producciones.findByEmpresa_IdEmpresaOrderByFechaDesc(empresa);
        float total=prod.stream().map(ProduccionMiel::getCantidad).filter(java.util.Objects::nonNull).reduce(0f,Float::sum);
        model.addAttribute("colmenas",colmenas.findByEmpresa_IdEmpresa(empresa));
        model.addAttribute("asociaciones",asociaciones.findByActivoTrue().stream().filter(a->empresa.equals(a.getIdEmpresa())).toList());
        model.addAttribute("tiposMedicion",TipoMedicion.values()); model.addAttribute("resultados",resultado); model.addAttribute("hayResultados",resultado.hasContent());
        model.addAttribute("producciones",prod); model.addAttribute("produccionTotal",total); model.addAttribute("produccionPromedio",prod.isEmpty()?0:total/prod.size());
        model.addAttribute("colmenasProduciendo",prod.stream().map(p->p.getColmena().getIdColmena()).distinct().count());
        model.addAttribute("alertasActivas",alertas.countByIdEmpresaAndEstado(empresa,"ACTIVA")); model.addAttribute("alertasTotal",(desde!=null&&hasta!=null)?alertas.countByIdEmpresaAndFechaBetween(empresa,desde,hasta):alertas.countByIdEmpresa(empresa));
        model.addAttribute("idAsociacion",idAsociacion);model.addAttribute("tipoMedicion",tipoMedicion);model.addAttribute("desde",desde);model.addAttribute("hasta",hasta);model.addAttribute("totalMediciones",resultado.getTotalElements());
        return "app/reportes";
    }
    @GetMapping("/exportar/{formato}")
    @PreAuthorize("hasRole('Admin_Cliente')")
    public void exportar(@PathVariable String formato,@RequestParam(required=false) Integer idAsociacion,@RequestParam(required=false) TipoMedicion tipoMedicion,
                          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
                          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,HttpServletResponse response)throws IOException{
        validarAsociacion(idAsociacion,contexto.getIdEmpresaActual()); List<Medicion> lista=mediciones.buscarReporte(contexto.getIdEmpresaActual(),idAsociacion,tipoMedicion,desde,hasta,PageRequest.of(0,10000)).getContent();
        if("pdf".equalsIgnoreCase(formato)){response.setContentType("application/pdf");response.setHeader("Content-Disposition","attachment; filename=\"reporte-apitech.pdf\"");reportes.generarPdf(lista,response.getOutputStream());}
        else if("excel".equalsIgnoreCase(formato)){response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");response.setHeader("Content-Disposition","attachment; filename=\"reporte-apitech.xlsx\"");reportes.generarExcel(lista,response.getOutputStream());}
        else if("csv".equalsIgnoreCase(formato)){response.setContentType("text/csv; charset=UTF-8");response.setCharacterEncoding("UTF-8");response.setHeader("Content-Disposition","attachment; filename=\"reporte-apitech.csv\"");var writer=response.getWriter();writer.write("\uFEFF");reportes.generarCsv(lista,writer);}
        else throw new IllegalArgumentException("Formato de reporte no soportado");
    }
    private void validarAsociacion(Integer id,Integer empresa){if(id!=null&&asociaciones.findById(id).map(a->!empresa.equals(a.getIdEmpresa())).orElse(true))throw new ReglaDeNegocioException("La asociación no pertenece a tu empresa");}
}
