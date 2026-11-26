package com.apitech.mk5.controller;

import com.apitech.mk5.entity.suscripcion.SolicitudSuscripcion;
import com.apitech.mk5.repository.suscripcion.PlanSuscripcionRepository;
import com.apitech.mk5.security.UsuarioContexto;
import com.apitech.mk5.service.SolicitudSuscripcionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class SolicitudSuscripcionController {
    private final SolicitudSuscripcionService service;
    private final PlanSuscripcionRepository planes;
    private final UsuarioContexto contexto;

    public SolicitudSuscripcionController(SolicitudSuscripcionService service,
                                          PlanSuscripcionRepository planes,
                                          UsuarioContexto contexto) {
        this.service = service;
        this.planes = planes;
        this.contexto = contexto;
    }

    @GetMapping("/solicitud-suscripcion")
    public String formulario(Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        model.addAttribute("planes", planes.findByEstado("activo").stream()
                .filter(p -> "Plan Básico".equalsIgnoreCase(p.getNombrePlan())
                        || "Plan Profesional".equalsIgnoreCase(p.getNombrePlan()))
                .sorted(java.util.Comparator.comparing(com.apitech.mk5.entity.suscripcion.PlanSuscripcion::getNombrePlan))
                .toList());
        return "auth/solicitud-suscripcion";
    }

    @PostMapping("/solicitud-suscripcion")
    public String crear(@RequestParam String nombreEmpresa, @RequestParam String nit,
                        @RequestParam String correoEmpresa, @RequestParam(required=false) String telefono,
                        @RequestParam(required=false) String direccion, @RequestParam String nombreContacto,
                        @RequestParam String correoContacto, @RequestParam(required=false) String telefonoContacto,
                        @RequestParam String tarjetaFalsa, @RequestParam Integer idPlan) {
        service.crear(nombreEmpresa, nit, correoEmpresa, telefono, direccion, nombreContacto,
                correoContacto, telefonoContacto, tarjetaFalsa, idPlan);
        return "redirect:/solicitud-suscripcion?enviada";
    }

    @GetMapping("/admin/solicitudes")
    @PreAuthorize("hasAnyRole('Admin_ApiTech','Empleado_ApiTech')")
    public String listar(Model model) {
        model.addAttribute("solicitudes", service.listarTodas());
        return "admin/solicitudes";
    }

    @GetMapping("/admin/solicitudes/{id}")
    @PreAuthorize("hasAnyRole('Admin_ApiTech','Empleado_ApiTech')")
    public String detalle(@PathVariable Integer id, Model model) {
        model.addAttribute("solicitud", service.obtener(id));
        model.addAttribute("planes", planes.findByEstado("activo"));
        return "admin/solicitud-detalle";
    }

    @PostMapping("/admin/solicitudes/{id}/editar")
    @PreAuthorize("hasAnyRole('Admin_ApiTech','Empleado_ApiTech')")
    public String editar(@PathVariable Integer id,
                         @RequestParam String nombreEmpresa, @RequestParam String nit,
                         @RequestParam String correoEmpresa, @RequestParam(required=false) String telefono,
                         @RequestParam(required=false) String direccion, @RequestParam String nombreContacto,
                         @RequestParam String correoContacto, @RequestParam(required=false) String telefonoContacto,
                         @RequestParam Integer idPlan) {
        service.actualizar(id, nombreEmpresa, nit, correoEmpresa, telefono, direccion,
                nombreContacto, correoContacto, telefonoContacto, idPlan);
        return "redirect:/admin/solicitudes/" + id + "?actualizada";
    }

    @PostMapping("/admin/solicitudes/{id}/aprobar")
    @PreAuthorize("hasAnyRole('Admin_ApiTech','Empleado_ApiTech')")
    public String aprobar(@PathVariable Integer id,
                          @RequestParam String nombreEmpresa, @RequestParam String nit,
                          @RequestParam String correoEmpresa, @RequestParam(required=false) String telefono,
                          @RequestParam(required=false) String direccion,
                          @RequestParam String correoAdmin, @RequestParam String passwordInicial,
                          @RequestParam Integer idPlan, @RequestParam(required=false) String metodoPago,
                          @RequestParam(required=false) String referenciaPago) {
        service.aprobar(id, contexto.getUsuarioActual().getIdUsuario(), nombreEmpresa, nit,
                correoEmpresa, telefono, direccion, correoAdmin, passwordInicial, idPlan,
                metodoPago, referenciaPago);
        return "redirect:/admin/solicitudes?aprobada";
    }

    @PostMapping("/admin/solicitudes/{id}/rechazar")
    @PreAuthorize("hasAnyRole('Admin_ApiTech','Empleado_ApiTech')")
    public String rechazar(@PathVariable Integer id, @RequestParam(required=false) String motivo) {
        service.rechazar(id, contexto.getUsuarioActual().getIdUsuario(), motivo);
        return "redirect:/admin/solicitudes?rechazada";
    }
}
