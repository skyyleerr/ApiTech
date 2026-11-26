package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.UsuarioRequestDTO;
import com.apitech.mk5.dto.request.PerfilUsuarioRequestDTO;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.repository.suscripcion.PagoRepository;
import com.apitech.mk5.repository.suscripcion.SuscripcionRepository;
import com.apitech.mk5.repository.usuario.RolRepository;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import com.apitech.mk5.security.UsuarioContexto;
import com.apitech.mk5.service.EmpresaService;
import com.apitech.mk5.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/** Vistas de administración interna de ApiTech. */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('Admin_ApiTech','Empleado_ApiTech')")
public class AdminManagementController {
    private final EmpresaRepository empresas;
    private final SuscripcionRepository suscripciones;
    private final PagoRepository pagos;
    private final EmpresaService empresaService;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final UsuarioService usuarioService;
    private final UsuarioContexto contexto;

    public AdminManagementController(EmpresaRepository empresas,
                                     SuscripcionRepository suscripciones,
                                     PagoRepository pagos,
                                     EmpresaService empresaService,
                                     UsuarioRepository usuarios,
                                     RolRepository roles,
                                     UsuarioService usuarioService,
                                     UsuarioContexto contexto) {
        this.empresas = empresas;
        this.suscripciones = suscripciones;
        this.pagos = pagos;
        this.empresaService = empresaService;
        this.usuarios = usuarios;
        this.roles = roles;
        this.usuarioService = usuarioService;
        this.contexto = contexto;
    }

    @GetMapping("/empresas")
    public String empresas(@RequestParam(required = false, defaultValue = "") String q, Model model) {
        var lista = q.isBlank()
                ? empresas.findAll()
                : empresas.findByNombreEmpresaContainingIgnoreCaseOrderByNombreEmpresaAsc(q.trim());
        model.addAttribute("empresas", lista);
        model.addAttribute("busqueda", q);
        return "admin/empresas";
    }

    @PostMapping("/empresas/estado/{id}")
    @PreAuthorize("hasRole('Admin_ApiTech')")
    public String estado(@PathVariable Integer id, @RequestParam String nuevoEstado) {
        empresaService.cambiarEstado(id, nuevoEstado);
        return "redirect:/admin/empresas";
    }

    @GetMapping("/usuarios")
    public String usuarios(Model model) {
        // Nombres de rol propios del equipo interno de ApiTech.
        java.util.List<String> nombresApiTech = java.util.List.of("Admin_ApiTech", "Empleado_ApiTech");

        // Se usa una consulta con JOIN FETCH (findByRolNombreInConRol) en vez de
        // usuarios.findAll() + filtro en Java: así se evita cualquier problema de
        // carga perezosa al leer usuario.rol.nombreRol en la plantilla, y el
        // listado no depende de que la sesión de Hibernate siga abierta.
        var usuariosApiTech = usuarios.findByRolNombreInConRol(nombresApiTech);
        var rolesApiTech = roles.findByNombreRolInOrderByNombreRolAsc(nombresApiTech);

        long administradores = usuariosApiTech.stream()
                .filter(u -> u.getRol() != null && "Admin_ApiTech".equalsIgnoreCase(u.getRol().getNombreRol()))
                .count();
        long empleados = usuariosApiTech.stream()
                .filter(u -> u.getRol() != null && "Empleado_ApiTech".equalsIgnoreCase(u.getRol().getNombreRol()))
                .count();

        model.addAttribute("usuarios", usuariosApiTech);
        model.addAttribute("roles", rolesApiTech);
        model.addAttribute("totalUsuarios", usuariosApiTech.size());
        model.addAttribute("totalAdministradores", administradores);
        model.addAttribute("totalEmpleados", empleados);
        model.addAttribute("hayRolesApiTech", !rolesApiTech.isEmpty());
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/crear")
    @PreAuthorize("hasRole('Admin_ApiTech')")
    public String crearUsuario(@RequestParam Integer idRol,
                               @RequestParam String nombre,
                               @RequestParam(required = false, defaultValue = "") String apellido,
                               @RequestParam String correo,
                               @RequestParam String password,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        usuarioService.crear(new UsuarioRequestDTO(null, idRol, nombre, apellido, correo, password, "activo"));
        redirect.addFlashAttribute("mensajeUsuarios", "Usuario creado correctamente.");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/estado")
    @PreAuthorize("hasRole('Admin_ApiTech')")
    public String cambiarEstadoUsuario(@PathVariable Integer id, @RequestParam String nuevoEstado,
                                       org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        if (contexto.getUsuarioActual().getIdUsuario().equals(id)) {
            throw new IllegalArgumentException("No puedes desactivar tu propio usuario.");
        }
        usuarioService.cambiarEstado(id, nuevoEstado);
        redirect.addFlashAttribute("mensajeUsuarios", "Estado del usuario actualizado.");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/editar")
    @PreAuthorize("hasRole('Admin_ApiTech')")
    public String editarUsuario(@PathVariable Integer id,
                                @RequestParam Integer idRol,
                                @RequestParam String nombre,
                                @RequestParam(required = false, defaultValue = "") String apellido,
                                @RequestParam String correo,
                                @RequestParam String estado,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        if (contexto.getUsuarioActual().getIdUsuario().equals(id) && !"activo".equalsIgnoreCase(estado)) {
            throw new IllegalArgumentException("No puedes desactivar tu propio usuario.");
        }
        usuarioService.actualizar(id, new com.apitech.mk5.dto.request.UsuarioActualizacionRequestDTO(
                null, idRol, nombre, apellido, correo, estado));
        redirect.addFlashAttribute("mensajeUsuarios", "Usuario actualizado correctamente.");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/eliminar")
    @PreAuthorize("hasRole('Admin_ApiTech')")
    public String eliminarUsuario(@PathVariable Integer id,
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        if (contexto.getUsuarioActual().getIdUsuario().equals(id)) {
            throw new IllegalArgumentException("No puedes eliminar tu propio usuario.");
        }
        boolean eliminadoDefinitivamente = usuarioService.eliminar(id);
        redirect.addFlashAttribute("mensajeUsuarios", eliminadoDefinitivamente
                ? "Usuario eliminado definitivamente."
                : "El usuario tiene historial asociado (auditoría, notificaciones), así que se desactivó en vez de eliminarse por completo, para no perder esos registros.");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/configuracion")
    public String configuracion(Model model) {
        var usuario = contexto.getUsuarioActual();
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("perfil", new PerfilUsuarioRequestDTO(
                usuario.getNombre(), usuario.getApellido(), usuario.getCorreo(), null));
        return "admin/configuracion";
    }

    @PostMapping("/configuracion/perfil")
    public String actualizarPerfil(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.ModelAttribute("perfil") PerfilUsuarioRequestDTO dto,
            org.springframework.validation.BindingResult result,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirect,
            Model model) {

        var actual = contexto.getUsuarioActual();
        if (result.hasErrors()) {
            model.addAttribute("usuarioActual", actual);
            model.addAttribute("perfil", dto);
            model.addAttribute("errorPerfil", "Revisa los datos ingresados. El nombre y el correo son obligatorios y deben ser válidos.");
            return "admin/configuracion";
        }

        try {
            String correoAnterior = actual.getCorreo();
            String rolActual = actual.getRol().getNombreRol();
            var actualizado = usuarioService.actualizarPerfil(actual.getIdUsuario(), dto);

            if (!correoAnterior.equalsIgnoreCase(actualizado.correo())) {
                var autoridades = java.util.List.of(new SimpleGrantedAuthority("ROLE_" + rolActual));
                var auth = new UsernamePasswordAuthenticationToken(actualizado.correo(), null, autoridades);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            redirect.addFlashAttribute("mensaje", "Perfil actualizado correctamente.");
            return "redirect:/admin/configuracion";
        } catch (RuntimeException ex) {
            model.addAttribute("usuarioActual", actual);
            model.addAttribute("perfil", dto);
            model.addAttribute("errorPerfil", ex.getMessage() == null ? "No fue posible actualizar tus datos." : ex.getMessage());
            return "admin/configuracion";
        }
    }

    @GetMapping("/pagos")
    public String pagos(Model model) {
        var lista = suscripciones.findAllByOrderByIdSuscripcionDesc();
        long pendientes = pagos.countByEstado(com.apitech.mk5.entity.suscripcion.Pago.EstadoPago.PENDIENTE);
        BigDecimal recibido = pagos.findAll().stream()
                .filter(p -> p.getEstado() == com.apitech.mk5.entity.suscripcion.Pago.EstadoPago.VERIFICADO)
                .map(com.apitech.mk5.entity.suscripcion.Pago::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("suscripciones", lista);
        model.addAttribute("pagosPendientes", pendientes);
        model.addAttribute("totalRecibido", recibido);
        return "admin/pagos";
    }

    /**
     * Maneja localmente los errores de negocio de ESTE controller
     * (crear/editar/eliminar/cambiar estado de usuarios y empresas).
     *
     * <p>Sin este manejador, cualquier {@link RuntimeException} lanzada
     * aquí (correo duplicado, intentar eliminarte a ti mismo, etc.) caía
     * en el {@code @RestControllerAdvice} global, que responde SIEMPRE
     * en JSON -- incluso para un {@code @Controller} clásico como este,
     * que debería devolver HTML. El resultado que veía la persona
     * usuaria era una página en blanco/JSON en vez de volver al listado
     * con un mensaje de error legible, lo que se sentía como "no
     * carga". Un {@code @ExceptionHandler} definido en el propio
     * controller tiene prioridad sobre el {@code @ControllerAdvice}
     * global, así que esto resuelve el problema solo para estas
     * pantallas, sin tocar el comportamiento de la API REST.</p>
     */
    @ExceptionHandler({
            com.apitech.mk5.exception.ReglaDeNegocioException.class,
            com.apitech.mk5.exception.RecursoNoEncontradoException.class,
            IllegalArgumentException.class,
            org.springframework.dao.DataIntegrityViolationException.class
    })
    public String manejarErrorDeGestion(RuntimeException ex,
                                        jakarta.servlet.http.HttpServletRequest request,
                                        org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        String mensaje = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "No fue posible completar la operación."
                : ex.getMessage();
        redirect.addFlashAttribute("mensajeUsuarios", mensaje);
        redirect.addFlashAttribute("errorUsuarios", true);
        redirect.addFlashAttribute("mensaje", mensaje);
        redirect.addFlashAttribute("errorPerfil", mensaje);

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/admin/")) {
            return "redirect:" + referer;
        }
        return "redirect:/admin/dashboard";
    }
}
