package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.SensorColmenaRequestDTO;
import com.apitech.mk5.dto.request.SensorRequestDTO;
import com.apitech.mk5.dto.request.UsuarioRequestDTO;
import com.apitech.mk5.dto.request.ProduccionMielRequestDTO;
import com.apitech.mk5.dto.request.CambioPasswordRequestDTO;
import com.apitech.mk5.dto.request.PerfilUsuarioRequestDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.entity.apiario.TipoMedicion;
import com.apitech.mk5.repository.apiario.AlertaRepository;
import com.apitech.mk5.repository.apiario.ColmenaRepository;
import com.apitech.mk5.repository.apiario.SensorRepository;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import com.apitech.mk5.security.UsuarioContexto;
import com.apitech.mk5.service.SensorColmenaService;
import com.apitech.mk5.service.SensorService;
import com.apitech.mk5.service.UsuarioService;
import com.apitech.mk5.service.ProduccionMielService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app")
@PreAuthorize("hasAnyRole('Admin_Cliente','Empleado_Cliente')")
public class ModulePageController {
    private final UsuarioContexto contexto;
    private final EmpresaRepository empresas;
    private final SensorRepository sensores;
    private final ColmenaRepository colmenas;
    private final UsuarioRepository usuarios;
    private final AlertaRepository alertas;
    private final SensorService sensorService;
    private final SensorColmenaService sensorColmenaService;
    private final UsuarioService usuarioService;
    private final ProduccionMielService produccionService;
    private final com.apitech.mk5.service.AlertaService alertaService;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;

    public ModulePageController(UsuarioContexto contexto, EmpresaRepository empresas,
                                SensorRepository sensores, ColmenaRepository colmenas,
                                UsuarioRepository usuarios, AlertaRepository alertas,
                                SensorService sensorService, SensorColmenaService sensorColmenaService,
                                UsuarioService usuarioService,
                                ProduccionMielService produccionService,
                                com.apitech.mk5.service.AlertaService alertaService,
                                com.apitech.mk5.service.EmpresaOperacionService empresaOperacion) {
        this.contexto = contexto;
        this.empresas = empresas;
        this.sensores = sensores;
        this.colmenas = colmenas;
        this.usuarios = usuarios;
        this.alertas = alertas;
        this.sensorService = sensorService;
        this.sensorColmenaService = sensorColmenaService;
        this.usuarioService = usuarioService;
        this.produccionService = produccionService;
        this.alertaService = alertaService;
        this.empresaOperacion = empresaOperacion;
    }

    @GetMapping({"/sensores", "/produccion", "/alertas", "/configuracion", "/usuarios", "/ayuda"})
    public String modulo(Model model, jakarta.servlet.http.HttpServletRequest request) {
        Integer empresa = empresaActual();
        String modulo = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
        model.addAttribute("modulo", modulo);
        model.addAttribute("titulo", titulo(modulo));
        model.addAttribute("empresaId", empresa);
        model.addAttribute("empresaPausada", contexto.getIdEmpresaActual() != null && empresas.findById(empresa).map(e -> !"activa".equalsIgnoreCase(e.getEstado())).orElse(false));
        model.addAttribute("limiteColmenas", empresaOperacion.limiteColmenas(empresa));
        model.addAttribute("limiteSensores", empresaOperacion.limiteSensores(empresa));
        model.addAttribute("sensores", sensores.findByEmpresa_IdEmpresa(empresa));
        model.addAttribute("colmenas", colmenas.findByEmpresa_IdEmpresa(empresa));
        model.addAttribute("usuarios", usuarios.findByEmpresa_IdEmpresa(empresa));
        model.addAttribute("alertas", alertas.findByIdEmpresaAndEstado(empresa, "ACTIVA", org.springframework.data.domain.PageRequest.of(0, 20)).getContent());
        if ("produccion".equals(modulo)) {
            var producciones = produccionService.listarPorEmpresa(empresa);
            model.addAttribute("producciones", producciones);
            double total = producciones.stream().mapToDouble(p -> p.cantidad()).sum();
            model.addAttribute("produccionTotal", total);
            model.addAttribute("produccionPromedio",
                    colmenas.findByEmpresa_IdEmpresa(empresa).isEmpty() ? 0
                            : total / colmenas.findByEmpresa_IdEmpresa(empresa).size());
        }
        if ("configuracion".equals(modulo)) {
            Usuario u = contexto.getUsuarioActual();
            model.addAttribute("usuarioActual", u);
            model.addAttribute("perfil", new PerfilUsuarioRequestDTO(
                    u.getNombre(), u.getApellido(), u.getCorreo(), null));
        }
        return "app/module";
    }

    @PostMapping("/sensores/registrar")
    @PreAuthorize("hasRole('Admin_Cliente')")
    public String registrarSensor(@RequestParam String codigo,
                                  @RequestParam TipoMedicion tipo,
                                  @RequestParam(required = false, defaultValue = "") String modelo,
                                  @RequestParam(required = false, defaultValue = "") String fabricante,
                                  RedirectAttributes redirect) {
        sensorService.crear(new SensorRequestDTO(empresaActual(), codigo, tipo,
                modelo, fabricante, true, "activo"));
        redirect.addFlashAttribute("mensaje", "Sensor registrado correctamente.");
        return "redirect:/app/sensores";
    }

    @PostMapping("/sensores/asociar")
    @PreAuthorize("hasRole('Admin_Cliente')")
    public String asociarSensor(@RequestParam Integer idSensor,
                                @RequestParam Integer idColmena,
                                RedirectAttributes redirect) {
        sensorColmenaService.crear(new SensorColmenaRequestDTO(idSensor, idColmena));
        redirect.addFlashAttribute("mensaje", "Sensor asociado a la colmena correctamente.");
        return "redirect:/app/sensores";
    }

    @PostMapping("/configuracion/perfil")
    public String actualizarPerfil(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.ModelAttribute("perfil") PerfilUsuarioRequestDTO dto,
            org.springframework.validation.BindingResult result,
            RedirectAttributes redirect,
            Model model) {
        Usuario actual = contexto.getUsuarioActual();
        if (result.hasErrors()) {
            model.addAttribute("modulo", "configuracion");
            model.addAttribute("titulo", "Configuración");
            model.addAttribute("empresaId", contexto.getIdEmpresaActual());
            model.addAttribute("usuarioActual", actual);
            model.addAttribute("perfil", dto);
            model.addAttribute("sensores", sensores.findByEmpresa_IdEmpresa(contexto.getIdEmpresaActual()));
            model.addAttribute("colmenas", colmenas.findByEmpresa_IdEmpresa(contexto.getIdEmpresaActual()));
            return "app/module";
        }

        String correoAnterior = actual.getCorreo();
        String rolActual = actual.getRol().getNombreRol();
        var actualizado = usuarioService.actualizarPerfil(actual.getIdUsuario(), dto);

        // Si cambió el correo, actualizamos el Authentication de la sesión.
        // De lo contrario, en la siguiente petición UsuarioContexto seguiría
        // buscando el correo anterior.
        if (!correoAnterior.equalsIgnoreCase(actualizado.correo())) {
            var autoridades = java.util.List.of(
                    new SimpleGrantedAuthority("ROLE_" + rolActual));
            var auth = new UsernamePasswordAuthenticationToken(
                    actualizado.correo(), null, autoridades);
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .setAuthentication(auth);
        }

        redirect.addFlashAttribute("mensaje", "Perfil actualizado correctamente.");
        return "redirect:/app/configuracion";
    }

    @PostMapping("/produccion/registrar")
    @PreAuthorize("hasRole('Admin_Cliente')")
    public String registrarProduccion(@RequestParam Integer idColmena,
                                      @RequestParam Float cantidad,
                                      @RequestParam(required = false, defaultValue = "kg") String unidad,
                                      @RequestParam java.time.LocalDate fecha,
                                      @RequestParam(required = false, defaultValue = "") String observaciones,
                                      RedirectAttributes redirect) {
        produccionService.crear(new ProduccionMielRequestDTO(
                idColmena, cantidad, unidad, fecha, observaciones));
        redirect.addFlashAttribute("mensaje", "Producción registrada correctamente.");
        return "redirect:/app/produccion";
    }

    @PostMapping("/usuarios/crear")
    @PreAuthorize("hasRole('Admin_Cliente')")
    public String crearUsuario(@RequestParam Integer idRol,
                               @RequestParam String nombre,
                               @RequestParam(required = false, defaultValue = "") String apellido,
                               @RequestParam String correo,
                               @RequestParam String password,
                               RedirectAttributes redirect) {
        usuarioService.crear(new UsuarioRequestDTO(empresaActual(), idRol, nombre,
                apellido, correo, password, "activo"));
        redirect.addFlashAttribute("mensaje", "Usuario creado correctamente.");
        return "redirect:/app/usuarios";
    }

    @PostMapping("/alertas/{id}/completar")
    public String completarAlerta(@PathVariable Integer id, RedirectAttributes redirect) {
        alertaService.resolver(id, contexto.getUsuarioActual().getIdUsuario());
        redirect.addFlashAttribute("mensaje", "Alerta marcada como completada.");
        return "redirect:/app/alertas";
    }

    private String titulo(String modulo) {
        return switch (modulo) {
            case "sensores" -> "Sensores";
            case "produccion" -> "Producción";
            case "alertas" -> "Alertas";
            case "configuracion" -> "Configuración";
            case "usuarios" -> "Usuarios";
            case "importar" -> "Importar Datos";
            default -> "Ayuda";
        };
    }

    private Integer empresaActual() {
        Integer id = contexto.getIdEmpresaActual();
        if (id != null) return id;
        return empresas.findAll().stream().findFirst().orElseThrow().getIdEmpresa();
    }
}
