package com.apitech.mk5.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.apitech.mk5.security.UsuarioContexto;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Controlador de autenticación y páginas de acceso.
 *
 * <p>Maneja la página de login y redirige al dashboard correcto
 * según el rol del usuario autenticado.</p>
 */
@Controller
public class AuthController {

    private final UsuarioContexto usuarioContexto;
    private final EmpresaRepository empresaRepository;

    public AuthController(UsuarioContexto usuarioContexto, EmpresaRepository empresaRepository) {
        this.usuarioContexto = usuarioContexto;
        this.empresaRepository = empresaRepository;
    }

    /** Muestra el formulario de login. */
    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String empresa,
            Model model, HttpServletResponse response) {

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (error != null) {
            model.addAttribute("errorMsg",
                "Correo o contraseña incorrectos, o cuenta inactiva.");
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", "Sesión cerrada correctamente.");
        }
        if (empresa != null) {
            empresaRepository.findByNit(empresa).ifPresent(e -> model.addAttribute("empresaNombre", e.getNombreEmpresa()));
        }
        return "auth/login";
    }

    @GetMapping("/login/empresa")
    public String empresa(Model model, HttpSession session) {
        session.removeAttribute("LOGIN_EMPRESA_ID");
        session.removeAttribute("LOGIN_EMPRESA_NIT");
        return "auth/empresa";
    }

    @PostMapping("/login/empresa")
    public String verificarEmpresa(@RequestParam String nit, Model model, HttpSession session) {
        return empresaRepository.findByNit(nit.trim())
                .map(e -> {
                    session.setAttribute("LOGIN_EMPRESA_ID", e.getIdEmpresa());
                    session.setAttribute("LOGIN_EMPRESA_NIT", e.getNit());
                    return "redirect:/login?empresa=" + e.getNit();
                })
                .orElseGet(() -> { model.addAttribute("errorMsg", "No encontramos una empresa con ese NIT."); return "auth/empresa"; });
    }

    /** Raíz: redirige al dashboard según el rol. */
    @GetMapping("/")
    public String raiz() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "landing";
        }
        if (usuarioContexto.tieneRol("Admin_ApiTech")
                || usuarioContexto.tieneRol("Empleado_ApiTech")) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/app/dashboard";
    }
}
