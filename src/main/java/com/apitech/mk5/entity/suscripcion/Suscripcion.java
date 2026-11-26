package com.apitech.mk5.entity.suscripcion;

import com.apitech.mk5.entity.empresa.Empresa;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Suscripción de una empresa a un plan ApiTech.
 * MySQL conserva el historial y usa activo_uniq para impedir dos suscripciones activas por empresa.
 */
@Entity
@Table(name = "suscripciones")
public class Suscripcion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suscripcion") private Integer idSuscripcion;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "id_empresa", nullable = false) private Empresa empresa;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "id_plan", nullable = false) private PlanSuscripcion plan;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoSuscripcion estado;
    /** TINYINT generado por MySQL: 1 para ACTIVA y NULL para cualquier otro estado. */
    @Column(name = "activo_uniq", insertable = false, updatable = false) private Byte activoUniq;
    @Column(name = "fecha_inicio") private LocalDate fechaInicio;
    @Column(name = "fecha_vencimiento") private LocalDate fechaVencimiento;
    @Column(name = "fecha_cancelacion") private LocalDate fechaCancelacion;
    @Column(name = "renovacion_automatica", nullable = false) private boolean renovacionAutomatica;
    @Column(name = "fecha_registro", insertable = false, updatable = false) private LocalDateTime fechaRegistro;
    @Column(name = "fecha_actualizacion", insertable = false, updatable = false) private LocalDateTime fechaActualizacion;

    protected Suscripcion() {}
    public Suscripcion(Empresa empresa, PlanSuscripcion plan) { this.empresa = empresa; this.plan = plan; this.estado = EstadoSuscripcion.PENDIENTE; this.renovacionAutomatica = false; }
    public Integer getIdSuscripcion() { return idSuscripcion; }
    public Empresa getEmpresa() { return empresa; }
    public PlanSuscripcion getPlan() { return plan; }
    public EstadoSuscripcion getEstado() { return estado; }
    public Byte getActivoUniq() { return activoUniq; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public LocalDate getFechaCancelacion() { return fechaCancelacion; }
    public boolean isRenovacionAutomatica() { return renovacionAutomatica; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setEstado(EstadoSuscripcion estado) { this.estado = estado; }
    /** Activa la suscripción y calcula su vigencia según el plan. */
    public void activar() { estado = EstadoSuscripcion.ACTIVA; fechaInicio = LocalDate.now(); fechaVencimiento = LocalDate.now().plusDays(plan.getDuracionDias()); }
    public enum EstadoSuscripcion { PENDIENTE, ACTIVA, SUSPENDIDA, VENCIDA, CANCELADA }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Suscripcion other)) return false; return idSuscripcion != null && idSuscripcion.equals(other.idSuscripcion); }
    @Override public int hashCode() { return Objects.hashCode(getClass()); }
}
