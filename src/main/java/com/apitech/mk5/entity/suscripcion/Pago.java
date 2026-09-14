package com.apitech.mk5.entity.suscripcion;

import com.apitech.mk5.entity.empresa.Empresa;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Pago asociado a una suscripción de una empresa.
 * Los pagos nacen pendientes y solo personal ApiTech puede verificarlos o rechazarlos.
 */
@Entity
@Table(name = "pagos")
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago") private Integer idPago;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false) private Empresa empresa;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_suscripcion", nullable = false) private Suscripcion suscripcion;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal valor;
    @Column(name = "fecha_pago", nullable = false) private LocalDate fechaPago;
    @Column(name = "metodo_pago", length = 50) private String metodoPago;
    @Column(length = 100) private String referencia;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoPago estado;
    @Column(name = "fecha_verificacion") private LocalDateTime fechaVerificacion;
    @Column(name = "id_usuario_verifico") private Integer idUsuarioVerifico;
    @Column(columnDefinition = "TEXT") private String observaciones;

    protected Pago() {}

    /** Crea una solicitud pendiente y exige que pago y suscripción sean del mismo tenant. */
    public Pago(Empresa empresa, Suscripcion suscripcion, BigDecimal valor, String metodoPago, String referencia) {
        if (empresa == null || suscripcion == null || suscripcion.getEmpresa() == null
                || !empresa.equals(suscripcion.getEmpresa())) {
            throw new IllegalArgumentException("El pago y la suscripcion deben pertenecer a la misma empresa");
        }
        this.empresa = empresa; this.suscripcion = suscripcion; this.valor = valor;
        this.metodoPago = metodoPago; this.referencia = referencia;
        this.fechaPago = LocalDate.now(); this.estado = EstadoPago.PENDIENTE;
    }

    public Integer getIdPago() { return idPago; }
    public Empresa getEmpresa() { return empresa; }
    public Suscripcion getSuscripcion() { return suscripcion; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getFechaPago() { return fechaPago; }
    public String getMetodoPago() { return metodoPago; }
    public String getReferencia() { return referencia; }
    public EstadoPago getEstado() { return estado; }
    public LocalDateTime getFechaVerificacion() { return fechaVerificacion; }
    public Integer getIdUsuarioVerifico() { return idUsuarioVerifico; }
    public String getObservaciones() { return observaciones; }
    public void verificar(Integer idUsuarioVerifico) {
        estado = EstadoPago.VERIFICADO;
        fechaVerificacion = LocalDateTime.now();
        this.idUsuarioVerifico = idUsuarioVerifico;
    }
    /** Compatibilidad con el flujo anterior; el nuevo flujo debe indicar el revisor. */
    public void verificar() { verificar(null); }
    public void rechazar() { estado = EstadoPago.RECHAZADO; }

    /** Estados permitidos por la columna ENUM del esquema definitivo. */
    public enum EstadoPago { PENDIENTE, VERIFICADO, RECHAZADO }

    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Pago other)) return false; return idPago != null && idPago.equals(other.idPago); }
    @Override public int hashCode() { return Objects.hashCode(getClass()); }
}
