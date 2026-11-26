package com.apitech.mk5.entity.suscripcion;

import com.apitech.mk5.entity.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Solicitud pública de alta. No representa todavía una empresa con acceso. */
@Entity
@Table(name = "solicitudes_suscripcion")
public class SolicitudSuscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    @Column(name = "nombre_empresa", nullable = false, length = 150)
    private String nombreEmpresa;
    @Column(name = "nit", nullable = false, length = 20)
    private String nit;
    @Column(name = "correo_empresa", nullable = false, length = 150)
    private String correoEmpresa;
    @Column(name = "telefono", length = 30)
    private String telefono;
    @Column(name = "direccion", length = 255)
    private String direccion;
    @Column(name = "nombre_contacto", nullable = false, length = 120)
    private String nombreContacto;
    @Column(name = "correo_contacto", nullable = false, length = 150)
    private String correoContacto;
    @Column(name = "telefono_contacto", length = 30)
    private String telefonoContacto;
    @Column(name = "tarjeta_falsa_ultimos4", length = 4)
    private String tarjetaFalsaUltimos4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan_solicitado")
    private PlanSuscripcion planSolicitado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;
    @Column(columnDefinition = "TEXT")
    private String observaciones;
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_revisor")
    private Usuario usuarioRevisor;
    @Column(name = "fecha_solicitud", insertable = false, updatable = false)
    private LocalDateTime fechaSolicitud;
    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;
    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    protected SolicitudSuscripcion() {}

    public SolicitudSuscripcion(String nombreEmpresa, String nit, String correoEmpresa,
                                String telefono, String direccion, String nombreContacto,
                                String correoContacto, String telefonoContacto, String tarjetaFalsaUltimos4,
                                PlanSuscripcion planSolicitado) {
        this.nombreEmpresa = nombreEmpresa;
        this.nit = nit;
        this.correoEmpresa = correoEmpresa;
        this.telefono = telefono;
        this.direccion = direccion;
        this.nombreContacto = nombreContacto;
        this.correoContacto = correoContacto;
        this.telefonoContacto = telefonoContacto;
        this.tarjetaFalsaUltimos4 = tarjetaFalsaUltimos4;
        this.planSolicitado = planSolicitado;
        this.estado = EstadoSolicitud.PENDIENTE;
    }

    public Integer getIdSolicitud() { return idSolicitud; }
    public String getNombreEmpresa() { return nombreEmpresa; }
    public String getNit() { return nit; }
    public String getCorreoEmpresa() { return correoEmpresa; }
    public String getTelefono() { return telefono; }
    public String getDireccion() { return direccion; }
    public String getNombreContacto() { return nombreContacto; }
    public String getCorreoContacto() { return correoContacto; }
    public String getTelefonoContacto() { return telefonoContacto; }
    public String getTarjetaFalsaUltimos4() { return tarjetaFalsaUltimos4; }
    public PlanSuscripcion getPlanSolicitado() { return planSolicitado; }
    public EstadoSolicitud getEstado() { return estado; }
    public String getObservaciones() { return observaciones; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public Usuario getUsuarioRevisor() { return usuarioRevisor; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }

    public void actualizarDatos(String nombreEmpresa, String nit, String correoEmpresa,
                                String telefono, String direccion, String nombreContacto,
                                String correoContacto, String telefonoContacto,
                                PlanSuscripcion plan) {
        this.nombreEmpresa = nombreEmpresa;
        this.nit = nit;
        this.correoEmpresa = correoEmpresa;
        this.telefono = telefono;
        this.direccion = direccion;
        this.nombreContacto = nombreContacto;
        this.correoContacto = correoContacto;
        this.telefonoContacto = telefonoContacto;
        this.tarjetaFalsaUltimos4 = tarjetaFalsaUltimos4;
        this.planSolicitado = plan;
    }

    public void aprobar(Usuario revisor) {
        this.estado = EstadoSolicitud.APROBADA;
        this.usuarioRevisor = revisor;
        this.fechaRevision = LocalDateTime.now();
        this.motivoRechazo = null;
    }

    public void rechazar(Usuario revisor, String motivo) {
        this.estado = EstadoSolicitud.RECHAZADA;
        this.usuarioRevisor = revisor;
        this.fechaRevision = LocalDateTime.now();
        this.motivoRechazo = motivo;
    }

    public enum EstadoSolicitud { PENDIENTE, APROBADA, RECHAZADA }
}
