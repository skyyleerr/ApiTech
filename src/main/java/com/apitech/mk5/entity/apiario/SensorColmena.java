package com.apitech.mk5.entity.apiario;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad JPA que mapea la tabla {@code sensor_colmena}.
 *
 * <p>Representa la asociacion HISTORICA entre un {@link Sensor} y una
 * {@link Colmena}: a diferencia de una relacion fija, un sensor puede
 * pasar por varias colmenas a lo largo del tiempo, y cada paso queda
 * registrado como una fila con {@code fechaInicio}/{@code fechaFin}.
 * Solo puede haber, como maximo, UNA asociacion activa por sensor a la
 * vez -- esa regla la impone la base de datos con la columna generada
 * {@code activo_uniq} + un indice unico sobre {@code (id_sensor,
 * activo_uniq)}, y debera reforzarse tambien desde el {@code service}
 * (no basta con dejar que la excepcion de base de datos sea la unica
 * red de seguridad, porque el mensaje de error de una violacion de
 * indice no es amigable para quien consuma la API).</p>
 *
 * <p><b>Sobre {@code idEmpresa}:</b> el esquema SQL denormaliza a
 * proposito el {@code id_empresa} en esta tabla (viene repetido, aunque
 * ya se puede deducir a traves de {@code sensor} o de {@code colmena}),
 * unicamente para poder declarar las FK compuestas
 * {@code fk_sc_sensor_empresa} / {@code fk_sc_colmena_empresa} que
 * garantizan en la base de datos que el sensor y la colmena de esta
 * asociacion pertenezcan a la MISMA empresa. Siguiendo la decision de
 * arquitectura ya acordada, este campo se mapea como una columna simple
 * ({@code Integer}), NO como una tercera relacion JPA -- la coherencia
 * "el id_empresa debe coincidir con el de sensor y colmena" se validara
 * explicitamente en el {@code service}, replicando lo que hoy hacen las
 * FK compuestas.</p>
 *
 * <p><b>Reglas de negocio pendientes de {@code service} (hoy en
 * triggers):</b></p>
 * <ul>
 *     <li>{@code trg_sensor_colmena_estado_bu}: no se puede desactivar
 *         ({@code activo=false}) una asociacion que tiene un
 *         {@link Monitoreo} activo -- primero hay que desactivar el
 *         monitoreo.</li>
 * </ul>
 */
@Entity
@Table(name = "sensor_colmena")
public class SensorColmena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asociacion")
    private Integer idAsociacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sensor", nullable = false)
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_colmena", nullable = false)
    private Colmena colmena;

    /**
     * Ver la nota de clase sobre por que este campo NO es una relacion
     * JPA, sino una columna simple denormalizada.
     */
    @Column(name = "id_empresa", nullable = false)
    private Integer idEmpresa;

    @Column(name = "fecha_inicio", insertable = false, updatable = false)
    private LocalDateTime fechaInicio;

    /**
     * Fecha en que la asociacion dejo de estar activa. {@code null}
     * mientras siga vigente. A diferencia de {@code fechaInicio}, este
     * campo SI lo controla la aplicacion (se establece explicitamente
     * al desactivar la asociacion), por eso no lleva
     * {@code insertable/updatable = false}.
     */
    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    /**
     * Columna generada por MySQL ({@code GENERATED ALWAYS AS (...) STORED}):
     * vale {@code 1} cuando {@code activo = true}, y {@code NULL} en
     * caso contrario. Existe solo para soportar el indice unico que
     * impide dos asociaciones activas simultaneas para el mismo sensor.
     * Se mapea de SOLO LECTURA ({@code insertable = false, updatable =
     * false}): la aplicacion nunca debe intentar escribirla, MySQL la
     * calcula sola a partir de {@code activo}.
     *
     * <p><b>Por que {@code Byte} y no {@code Integer}:</b> la columna
     * real en MySQL/MariaDB es {@code TINYINT}, no {@code INT}. Con
     * {@code Integer}, Hibernate 7 (el que trae Spring Boot 4.1.1)
     * rechaza el arranque en la validacion de esquema porque
     * {@code TINYINT} y {@code INTEGER} ya no se consideran
     * equivalentes automaticamente. {@code Byte} es el tipo Java que
     * corresponde exactamente a un {@code TINYINT} de un solo byte.</p>
     */
    @Column(name = "activo_uniq", insertable = false, updatable = false)
    private Byte activoUniq;

    /** Constructor vacio requerido por JPA. */
    protected SensorColmena() {
    }

    /**
     * Crea una asociacion nueva, activa por defecto (coincide con el
     * {@code DEFAULT TRUE} de la columna {@code activo} en SQL).
     */
    public SensorColmena(Sensor sensor, Colmena colmena, Integer idEmpresa) {
        this.sensor = sensor;
        this.colmena = colmena;
        this.idEmpresa = idEmpresa;
        this.activo = true;
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Integer getIdAsociacion() {
        return idAsociacion;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
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

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Byte getActivoUniq() {
        return activoUniq;
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SensorColmena other)) return false;
        return idAsociacion != null && idAsociacion.equals(other.idAsociacion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    @Override
    public String toString() {
        return "SensorColmena{" +
                "idAsociacion=" + idAsociacion +
                ", activo=" + activo +
                '}';
    }
}
