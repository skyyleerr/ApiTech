package com.apitech.mk5.entity.evento;

import com.apitech.mk5.entity.apiario.Colmena;
import com.apitech.mk5.entity.empresa.Empresa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/** Registro de una cosecha de miel asociada a una colmena. */
@Entity
@Table(name = "produccion_miel")
public class ProduccionMiel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produccion")
    private Integer idProduccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_colmena", nullable = false)
    private Colmena colmena;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @Column(name = "cantidad", nullable = false)
    private Float cantidad;

    @Column(name = "unidad", nullable = false, length = 10)
    private String unidad;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    protected ProduccionMiel() {
    }

    public ProduccionMiel(Colmena colmena, Empresa empresa, Float cantidad, String unidad,
                          LocalDate fecha, String observaciones) {
        this.colmena = colmena;
        this.empresa = empresa;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.fecha = fecha;
        this.observaciones = observaciones;
    }

    public Integer getIdProduccion() { return idProduccion; }
    public Colmena getColmena() { return colmena; }
    public void setColmena(Colmena colmena) { this.colmena = colmena; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Float getCantidad() { return cantidad; }
    public void setCantidad(Float cantidad) { this.cantidad = cantidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProduccionMiel other)) return false;
        return idProduccion != null && idProduccion.equals(other.idProduccion);
    }

    @Override
    public int hashCode() { return Objects.hashCode(getClass()); }
}
