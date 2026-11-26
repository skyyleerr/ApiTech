package com.apitech.mk5.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.security.UsuarioContexto;

/**
 * Dashboard del equipo interno ApiTech.
 *
 * <p>Solo accesible para {@code Admin_ApiTech} y
 * {@code Empleado_ApiTech}. Muestra resumen de empresas,
 * suscripciones y pagos pendientes.</p>
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('Admin_ApiTech','Empleado_ApiTech')")
public class AdminDashboardController {

    private final EmpresaRepository empresaRepository;
    private final UsuarioContexto usuarioContexto;

    public AdminDashboardController(EmpresaRepository empresaRepository,
                                    UsuarioContexto usuarioContexto) {
        this.empresaRepository = empresaRepository;
        this.usuarioContexto = usuarioContexto;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("usuario", usuarioContexto.getUsuarioActual());
        model.addAttribute("totalEmpresas",
                empresaRepository.count());
        model.addAttribute("empresasActivas",
                empresaRepository.findAll().stream()
                        .filter(e -> "activa".equals(e.getEstado()))
                        .count());
        if (usuarioContexto.tieneRol("Admin_ApiTech")) {
            return "admin/dashboard-admin-apitech";
        }
        return "admin/dashboard-empleado-apitech";
    }
}
