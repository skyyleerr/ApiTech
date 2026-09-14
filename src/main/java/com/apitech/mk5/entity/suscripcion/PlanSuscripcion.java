package com.apitech.mk5.entity.suscripcion;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

/** Plan comercial con precio y duración para las suscripciones ApiTech. */
@Entity
@Table(name = "planes_suscripcion")
public class PlanSuscripcion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan") private Integer idPlan;
    @Column(name = "nombre_plan", nullable = false, length = 100) private String nombrePlan;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal precio;
    @Column(name = "duracion_dias", nullable = false) private Integer duracionDias;
    @Column(nullable = false, length = 20) private String estado;
    protected PlanSuscripcion() {}
    public PlanSuscripcion(String nombrePlan, BigDecimal precio, Integer duracionDias, String estado) { this.nombrePlan = nombrePlan; this.precio = precio; this.duracionDias = duracionDias; this.estado = estado; }
    public Integer getIdPlan() { return idPlan; }
    public String getNombrePlan() { return nombrePlan; }
    public BigDecimal getPrecio() { return precio; }
    public Integer getDuracionDias() { return duracionDias; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof PlanSuscripcion other)) return false; return idPlan != null && idPlan.equals(other.idPlan); }
    @Override public int hashCode() { return Objects.hashCode(getClass()); }
}
