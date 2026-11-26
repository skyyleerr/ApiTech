package com.apitech.mk5.entity.apiario;

import com.apitech.mk5.entity.usuario.Usuario;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad JPA que mapea la tabla {@code alertas}.
 *
 * <p>Evento generado cuando una {@link Medicion} cae fuera del rango
 * definido por un {@link RangoUmbral} para su tipo. Cierra el ciclo
 * central del sistema: Sensor -> Medicion -> (fuera de rango) -> Alerta
 * -> Notificacion (esta ultima, fuera del alcance de este nucleo).</p>
 *
 * <p><b>Sobre {@code idEmpresa}:</b> igual que en {@link SensorColmena},
 * es una columna simple denormalizada (no una relacion JPA), que existe
 * solo para que la base de datos pueda validar con una FK compuesta que
 * la empresa de la alerta coincide con la de su {@code colmena}. La
 * coherencia real se valida en el {@code service}.</p>
 *
 * <p><b>Campos snapshot ({@code valorDetectado}, {@code umbralMin},
 * {@code umbralMax}):</b> son {@code Float} (objeto, no {@code float}
 * primitivo) porque la columna SQL admite {@code NULL} -- capturan una
 * "foto" del valor y el umbral vigente EN EL MOMENTO en que se genero
 * la alerta, para que si el umbral cambia despues, el historial de la
 * alerta siga reflejando las condiciones reales bajo las que se
 * disparo.</p>
 *
 * <p><b>Reglas de negocio pendientes de {@code service} (hoy en
 * {@code trg_alertas_empresa_bi/bu} y {@code trg_alertas_integridad_bi/bu}):</b></p>
 * <ul>
 *     <li>{@code idEmpresa} debe coincidir con {@code colmena.empresa.idEmpresa}.</li>
 *     <li>Si se indica {@code sensor}, debe tener (o haber tenido) una
 *         asociacion activa con {@code colmena}.</li>
 *     <li>Si se indica {@code medicion}, su asociacion debe corresponder
 *         a la misma {@code colmena} (y {@code sensor}, si tambien se indico).</li>
 * </ul>
 */
@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer idAlerta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_colmena", nullable = false)
    private Colmena colmena;

    /** Ver la nota de clase: columna simple, no relacion JPA. */
    @Column(name = "id_empresa", nullable = false)
    private Integer idEmpresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medicion", nullable = true)
    private Medicion medicion;

    /**
     * Redundante con {@code medicion.asociacion.sensor} cuando
     * {@code medicion} no es {@code null}, pero util cuando SI es
     * {@code null} (por ejemplo, una alerta generada por una condicion
     * distinta a una lectura puntual).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sensor", nullable = true)
    private Sensor sensor;

    @Column(name = "tipo", nullable = false, length = 100)
    private String tipo;

    /**
     * {@code length = 65535} (el maximo de un {@code TEXT} de MySQL) es
     * el detalle que evita otro error de validacion de esquema: sin
     * especificar una longitud, {@code @Lob} asume por defecto 255
     * caracteres, y con eso Hibernate 7 espera un {@code TINYTEXT} en
     * vez del {@code TEXT} real que define el script SQL.
     */
    @Lob
    @Column(name = "descripcion", length = 65535)
    private String descripcion;

    @Column(name = "valor_detectado")
    private Float valorDetectado;

    @Column(name = "umbral_min")
    private Float umbralMin;

    @Column(name = "umbral_max")
    private Float umbralMax;

    @Column(name = "severidad", nullable = false, length = 20)
    private String severidad;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha", insertable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_resolucion", nullable = true)
    private Usuario usuarioResolucion;

    /** Constructor vacio requerido por JPA. */
    protected Alerta() {
    }

    /**
     * Crea una alerta nueva (sin resolver todavia: {@code fechaResolucion}
     * y {@code usuarioResolucion} quedan en {@code null} hasta que se
     * resuelva explicitamente).
     */
    public Alerta(Colmena colmena, Integer idEmpresa, Medicion medicion, Sensor sensor,
                   String tipo, String descripcion, Float valorDetectado,
                   Float umbralMin, Float umbralMax, String severidad, String estado) {
        this.colmena = colmena;
        this.idEmpresa = idEmpresa;
        this.medicion = medicion;
        this.sensor = sensor;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.valorDetectado = valorDetectado;
        this.umbralMin = umbralMin;
        this.umbralMax = umbralMax;
        this.severidad = severidad;
        this.estado = estado;
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Integer getIdAlerta() {
        return idAlerta;
    }

    public Colmena getColmena() {
        return colmena;
    }

    public void setColmena(Colmena colmena) {
        this.colmena = colmena;
    }

    public Integer getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Integer idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Medicion getMedicion() {
        return medicion;
    }

    public void setMedicion(Medicion medicion) {
        this.medicion = medicion;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Float getValorDetectado() {
        return valorDetectado;
    }

    public void setValorDetectado(Float valorDetectado) {
        this.valorDetectado = valorDetectado;
    }

    public Float getUmbralMin() {
        return umbralMin;
    }

    public void setUmbralMin(Float umbralMin) {
        this.umbralMin = umbralMin;
    }

    public Float getUmbralMax() {
        return umbralMax;
    }

    public void setUmbralMax(Float umbralMax) {
        this.umbralMax = umbralMax;
    }

    public String getSeveridad() {
        return severidad;
    }

    public void setSeveridad(String severidad) {
        this.severidad = severidad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public Usuario getUsuarioResolucion() {
        return usuarioResolucion;
    }

    public void setUsuarioResolucion(Usuario usuarioResolucion) {
        this.usuarioResolucion = usuarioResolucion;
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alerta other)) return false;
        return idAlerta != null && idAlerta.equals(other.idAlerta);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    @Override
    public String toString() {
        return "Alerta{" +
                "idAlerta=" + idAlerta +
                ", tipo='" + tipo + '\'' +
                ", severidad='" + severidad + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}
