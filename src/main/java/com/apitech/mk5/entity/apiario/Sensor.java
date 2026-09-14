package com.apitech.mk5.entity.apiario;

import com.apitech.mk5.entity.empresa.Empresa;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad JPA que mapea la tabla {@code sensores}.
 *
 * <p>Representa un dispositivo (fisico o simulado) capaz de generar
 * {@code mediciones} de un tipo especifico. Un sensor pertenece a una
 * empresa, pero NO pertenece directamente a una colmena -- esa relacion
 * es historica y vive en la tabla {@code sensor_colmena} (con fechas de
 * inicio/fin), que se incorporara en la proxima etapa. Un mismo sensor
 * podria, con el tiempo, pasar por varias colmenas.</p>
 */
@Entity
@Table(name = "sensores")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sensor")
    private Integer idSensor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    /**
     * Tipo de medicion que produce este sensor. A diferencia de
     * {@code Colmena.estado}, aqui SI se usa un {@code enum} de Java
     * ({@link TipoMedicion}), porque la columna es un {@code ENUM} real
     * en MySQL -- Java refleja la misma restriccion que ya impone la
     * base de datos, en vez de aceptar cualquier texto.
     *
     * <p>{@code @Enumerated(EnumType.STRING)} le dice a Hibernate que
     * guarde y lea el NOMBRE de la constante (por ejemplo
     * {@code "TEMPERATURA"}), no su posicion numerica ({@code 0},
     * {@code 1}, {@code 2}). Es fundamental usar {@code STRING} y no el
     * valor por defecto ({@code ORDINAL}): si en el futuro alguien
     * reordenara las constantes del enum {@link TipoMedicion}, con
     * {@code ORDINAL} los datos ya guardados cambiarian de significado
     * silenciosamente; con {@code STRING} eso no puede pasar.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoMedicion tipo;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "fabricante", length = 100)
    private String fabricante;

    /**
     * {@code true} = sensor simulado (genera datos sinteticos para
     * pruebas/demo); {@code false} = sensor fisico IoT real (ESP32,
     * etc.). Esta bandera es la que, mas adelante, el trigger
     * {@code trg_mediciones_coherencia_bi/bu} usa para exigir que el
     * {@code origen} de cada medicion coincida (SIMULADO/REAL) -- esa
     * validacion cruzada se reforzara en el {@code service} de
     * {@code Medicion} cuando lleguemos a esa entidad.
     */
    @Column(name = "es_simulado", nullable = false)
    private boolean esSimulado;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_activacion")
    private LocalDateTime fechaActivacion;

    @Column(name = "fecha_desactivacion")
    private LocalDateTime fechaDesactivacion;

    /** Constructor vacio requerido por JPA. */
    protected Sensor() {
    }

    public Sensor(Empresa empresa, String codigo, TipoMedicion tipo, String modelo,
                  String fabricante, boolean esSimulado, String estado) {
        this.empresa = empresa;
        this.codigo = codigo;
        this.tipo = tipo;
        this.modelo = modelo;
        this.fabricante = fabricante;
        this.esSimulado = esSimulado;
        this.estado = estado;
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Integer getIdSensor() {
        return idSensor;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public TipoMedicion getTipo() {
        return tipo;
    }

    public void setTipo(TipoMedicion tipo) {
        this.tipo = tipo;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getFabricante() {
        return fabricante;
    }

    public void setFabricante(String fabricante) {
        this.fabricante = fabricante;
    }

    public boolean isEsSimulado() {
        return esSimulado;
    }

    public void setEsSimulado(boolean esSimulado) {
        this.esSimulado = esSimulado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public LocalDateTime getFechaActivacion() {
        return fechaActivacion;
    }

    public void setFechaActivacion(LocalDateTime fechaActivacion) {
        this.fechaActivacion = fechaActivacion;
    }

    public LocalDateTime getFechaDesactivacion() {
        return fechaDesactivacion;
    }

    public void setFechaDesactivacion(LocalDateTime fechaDesactivacion) {
        this.fechaDesactivacion = fechaDesactivacion;
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sensor other)) return false;
        return idSensor != null && idSensor.equals(other.idSensor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "idSensor=" + idSensor +
                ", codigo='" + codigo + '\'' +
                ", tipo=" + tipo +
                ", estado='" + estado + '\'' +
                '}';
    }
}
