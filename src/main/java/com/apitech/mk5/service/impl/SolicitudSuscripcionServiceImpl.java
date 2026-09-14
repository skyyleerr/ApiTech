package com.apitech.mk5.service.impl;

import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.suscripcion.PlanSuscripcion;
import com.apitech.mk5.entity.suscripcion.SolicitudSuscripcion;
import com.apitech.mk5.entity.usuario.Rol;
import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.repository.suscripcion.PlanSuscripcionRepository;
import com.apitech.mk5.repository.suscripcion.SolicitudSuscripcionRepository;
import com.apitech.mk5.repository.usuario.RolRepository;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import com.apitech.mk5.service.PagoService;
import com.apitech.mk5.service.SolicitudSuscripcionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SolicitudSuscripcionServiceImpl implements SolicitudSuscripcionService {
    private final SolicitudSuscripcionRepository solicitudes;
    private final PlanSuscripcionRepository planes;
    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final PagoService pagoService;

    public SolicitudSuscripcionServiceImpl(SolicitudSuscripcionRepository solicitudes,
                                           PlanSuscripcionRepository planes,
                                           EmpresaRepository empresas,
                                           UsuarioRepository usuarios,
                                           RolRepository roles,
                                           PasswordEncoder encoder,
                                           PagoService pagoService) {
        this.solicitudes = solicitudes;
        this.planes = planes;
        this.empresas = empresas;
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.pagoService = pagoService;
    }

    @Override
    @Transactional
    public SolicitudSuscripcion crear(String nombreEmpresa, String nit, String correoEmpresa,
                                      String telefono, String direccion, String nombreContacto,
                                      String correoContacto, String telefonoContacto, String tarjetaFalsa, Integer idPlan) {
        String nitLimpio = limpiar(nit);
        if (empresas.existsByNit(nitLimpio))
            throw new ReglaDeNegocioException("Ya existe una empresa registrada con el NIT " + nitLimpio);
        if (solicitudes.existsByNitAndEstado(nitLimpio, SolicitudSuscripcion.EstadoSolicitud.PENDIENTE))
            throw new ReglaDeNegocioException("Ya existe una solicitud pendiente para ese NIT");
        PlanSuscripcion plan = plan(idPlan);
        String tarjeta = tarjetaFalsa == null ? "" : tarjetaFalsa.replaceAll("\\D", "");
        if (tarjeta.length() < 12 || tarjeta.length() > 19)
            throw new ReglaDeNegocioException("La tarjeta de débito falsa debe tener entre 12 y 19 dígitos.");
        String ultimos4 = tarjeta.substring(tarjeta.length() - 4);
        return solicitudes.save(new SolicitudSuscripcion(nombreEmpresa.trim(), nitLimpio,
                correoEmpresa.trim(), telefono, direccion, nombreContacto.trim(),
                correoContacto.trim(), telefonoContacto, ultimos4, plan));
    }

    @Override public List<SolicitudSuscripcion> listarTodas() { return solicitudes.findAllByOrderByIdSolicitudDesc(); }
    @Override public List<SolicitudSuscripcion> listarPendientes() { return solicitudes.findByEstadoOrderByIdSolicitudDesc(SolicitudSuscripcion.EstadoSolicitud.PENDIENTE); }
    @Override public SolicitudSuscripcion obtener(Integer id) { return buscar(id); }

    @Override
    @Transactional
    public SolicitudSuscripcion actualizar(Integer id, String nombreEmpresa, String nit,
                                           String correoEmpresa, String telefono, String direccion,
                                           String nombreContacto, String correoContacto,
                                           String telefonoContacto, Integer idPlan) {
        SolicitudSuscripcion s = buscar(id);
        if (s.getEstado() != SolicitudSuscripcion.EstadoSolicitud.PENDIENTE)
            throw new ReglaDeNegocioException("Solo se pueden editar solicitudes pendientes");
        String nitLimpio = limpiar(nit);
        empresas.findByNit(nitLimpio).ifPresent(e -> { throw new ReglaDeNegocioException("El NIT ya pertenece a una empresa registrada"); });
        if (solicitudes.existsByNitAndEstadoAndIdSolicitudNot(nitLimpio, SolicitudSuscripcion.EstadoSolicitud.PENDIENTE, id))
            throw new ReglaDeNegocioException("Ya existe otra solicitud pendiente para ese NIT");
        PlanSuscripcion plan = plan(idPlan);
        s.actualizarDatos(nombreEmpresa.trim(), nitLimpio, correoEmpresa.trim(), telefono, direccion,
                nombreContacto.trim(), correoContacto.trim(), telefonoContacto, plan);
        return s;
    }

    @Override
    @Transactional
    public void rechazar(Integer id, Integer idUsuarioRevisor, String motivo) {
        SolicitudSuscripcion s = buscar(id);
        if (s.getEstado() != SolicitudSuscripcion.EstadoSolicitud.PENDIENTE)
            throw new ReglaDeNegocioException("La solicitud ya fue procesada");
        s.rechazar(usuario(idUsuarioRevisor), motivo == null ? "Solicitud rechazada por ApiTech" : motivo.trim());
    }

    @Override
    @Transactional
    public void aprobar(Integer id, Integer idUsuarioRevisor, String nombreEmpresa, String nit,
                        String correoEmpresa, String telefono, String direccion,
                        String correoAdmin, String passwordInicial, Integer idPlan,
                        String metodoPago, String referenciaPago) {
        SolicitudSuscripcion s = buscar(id);
        if (s.getEstado() != SolicitudSuscripcion.EstadoSolicitud.PENDIENTE)
            throw new ReglaDeNegocioException("La solicitud ya fue procesada");
        if (passwordInicial == null || passwordInicial.length() < 8)
            throw new ReglaDeNegocioException("La contraseña inicial debe tener al menos 8 caracteres");
        Usuario revisor = usuario(idUsuarioRevisor);
        PlanSuscripcion plan = plan(idPlan);
        String nitLimpio = limpiar(nit);
        String correoAdminLimpio = limpiar(correoAdmin);
        if (empresas.existsByNit(nitLimpio))
            throw new ReglaDeNegocioException("Ya existe una empresa registrada con el NIT " + nitLimpio);
        if (usuarios.existsByCorreo(correoAdminLimpio))
            throw new ReglaDeNegocioException("El correo del administrador ya está registrado");

        Empresa empresa = empresas.save(new Empresa(nitLimpio, nombreEmpresa.trim(),
                correoEmpresa.trim(), telefono, direccion, "activa"));
        Rol rolAdminCliente = roles.findByNombreRol("Admin_Cliente")
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el rol Admin_Cliente"));
        Usuario adminCliente = new Usuario(empresa, rolAdminCliente,
                s.getNombreContacto(), "", correoAdminLimpio,
                encoder.encode(passwordInicial), "activo");
        usuarios.save(adminCliente);

        var pago = pagoService.checkout(empresa, plan.getIdPlan(),
                metodoPago == null ? "MANUAL" : metodoPago.trim(),
                referenciaPago == null ? "ALTA-" + s.getIdSolicitud() : referenciaPago.trim());
        pagoService.verificar(pago.getIdPago(), revisor.getIdUsuario());
        s.actualizarDatos(nombreEmpresa.trim(), nitLimpio, correoEmpresa.trim(), telefono, direccion,
                s.getNombreContacto(), s.getCorreoContacto(), s.getTelefonoContacto(), plan);
        s.aprobar(revisor);
    }

    private SolicitudSuscripcion buscar(Integer id) {
        return solicitudes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la solicitud " + id));
    }
    private PlanSuscripcion plan(Integer id) {
        if (id == null) throw new ReglaDeNegocioException("Debe seleccionar un plan");
        return planes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe el plan " + id));
    }
    private Usuario usuario(Integer id) { return usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario revisor " + id)); }
    private String limpiar(String valor) { return valor == null ? "" : valor.trim(); }
}
