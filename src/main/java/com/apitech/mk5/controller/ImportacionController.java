package com.apitech.mk5.controller;

import com.apitech.mk5.dto.response.ImportacionResultadoDTO;
import com.apitech.mk5.service.ImportacionService;
import com.apitech.mk5.repository.apiario.ColmenaRepository;
import com.apitech.mk5.repository.apiario.SensorColmenaRepository;
import com.apitech.mk5.security.UsuarioContexto;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/app/importar")
@PreAuthorize("hasAnyRole('Admin_Cliente','Empleado_Cliente')")
public class ImportacionController {
    private static final String PREVIA = "APITECH_IMPORTACION_PREVIA";
    private final ImportacionService service;
    private final ColmenaRepository colmenas;
    private final SensorColmenaRepository asociaciones;
    private final UsuarioContexto contexto;
    public ImportacionController(ImportacionService service, ColmenaRepository colmenas,
                                 SensorColmenaRepository asociaciones, UsuarioContexto contexto) {
        this.service = service; this.colmenas = colmenas; this.asociaciones = asociaciones; this.contexto = contexto;
    }

    @GetMapping
    public String formulario(Model model, HttpSession session) {
        model.addAttribute("resultado", session.getAttribute(PREVIA));
        return "app/importar";
    }

    @PostMapping("/previsualizar")
    public String previsualizar(@RequestParam("archivo") MultipartFile archivo, HttpSession session, Model model) {
        try { ImportacionResultadoDTO r=service.previsualizar(archivo); session.setAttribute(PREVIA,r); model.addAttribute("resultado",r); }
        catch (RuntimeException e) { model.addAttribute("error", e.getMessage()); }
        return "app/importar";
    }

    @PostMapping("/confirmar")
    public String confirmar(HttpSession session, Model model) {
        try { ImportacionResultadoDTO r=service.confirmar((ImportacionResultadoDTO)session.getAttribute(PREVIA)); session.setAttribute(PREVIA,r); model.addAttribute("resultado",r); model.addAttribute("exito","Importación completada correctamente."); }
        catch (RuntimeException e) { model.addAttribute("error",e.getMessage()); model.addAttribute("resultado",session.getAttribute(PREVIA)); }
        return "app/importar";
    }

    @GetMapping("/plantilla.csv")
    public void plantillaCsv(HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"plantilla-apitech.csv\"");
        response.getWriter().print("\uFEFF");
        int idColmena = colmenas.findByEmpresa_IdEmpresa(contexto.getIdEmpresaActual()).stream().findFirst().map(c -> c.getIdColmena()).orElse(1);
        var asociacion = asociaciones.findByActivoTrue().stream().filter(a -> contexto.getIdEmpresaActual().equals(a.getIdEmpresa())).findFirst().orElse(null);
        int idAsociacion = asociacion == null ? 1 : asociacion.getIdAsociacion();
        String tipo = asociacion == null ? "TEMPERATURA" : asociacion.getSensor().getTipo().name();
        String unidad = asociacion == null ? "°C" : (switch (asociacion.getSensor().getTipo()) { case TEMPERATURA -> "°C"; case HUMEDAD -> "%"; case PESO -> "kg"; });
        String origen = asociacion == null || asociacion.getSensor().isEsSimulado() ? "SIMULADO" : "REAL";
        response.getWriter().print("fecha,id_colmena,id_asociacion,tipo_medicion,valor,unidad,origen,produccion,observaciones\n");
        response.getWriter().print("2026-09-01," + idColmena + "," + idAsociacion + "," + tipo + ",28.5," + unidad + "," + origen + ",,Lectura de ejemplo\n");
        response.getWriter().print("2026-09-01," + idColmena + ",,,,,,12.5,Cosecha de ejemplo\n");
        response.getWriter().flush();
    }

    @GetMapping("/plantilla.xlsx")
    public void plantillaXlsx(HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"plantilla-apitech.xlsx\"");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Importacion");
            String[] headers = {"fecha","id_colmena","id_asociacion","tipo_medicion","valor","unidad","origen","produccion","observaciones"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            int idColmena = colmenas.findByEmpresa_IdEmpresa(contexto.getIdEmpresaActual()).stream().findFirst().map(c -> c.getIdColmena()).orElse(1);
            var asociacion = asociaciones.findByActivoTrue().stream().filter(a -> contexto.getIdEmpresaActual().equals(a.getIdEmpresa())).findFirst().orElse(null);
            String unidad = asociacion == null ? "°C" : switch (asociacion.getSensor().getTipo()) { case TEMPERATURA -> "°C"; case HUMEDAD -> "%"; case PESO -> "kg"; };
            String[] example = {"2026-09-01",String.valueOf(idColmena),String.valueOf(asociacion == null ? 1 : asociacion.getIdAsociacion()),asociacion == null ? "TEMPERATURA" : asociacion.getSensor().getTipo().name(),"28.5",unidad,asociacion == null || asociacion.getSensor().isEsSimulado() ? "SIMULADO" : "REAL","","Lectura de ejemplo"};
            Row row = sheet.createRow(1);
            for (int i = 0; i < example.length; i++) row.createCell(i).setCellValue(example[i]);
            String[] production = {"2026-09-01",String.valueOf(idColmena),"","","","","","12.5","Cosecha de ejemplo"};
            row = sheet.createRow(2);
            for (int i = 0; i < production.length; i++) row.createCell(i).setCellValue(production[i]);
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(response.getOutputStream());
        }
    }
}
