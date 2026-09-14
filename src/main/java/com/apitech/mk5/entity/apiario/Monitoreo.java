package com.apitech.mk5.entity.apiario;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad JPA que mapea la tabla {@code monitoreo}.
 *
 * <p>Representa la ACTIVACION de una asociacion sensor-colmena: mientras
 * una fila de {@link SensorColmena} dice "este sensor esta (o estuvo)
 * instalado en esta colmena", una fila de {@code Monitoreo} dice "en
 * este periodo, esa instalacion estuvo efectivamente monitoreandose"
 * (es decir, generando/aceptando {@code mediciones}). Es una capa mas
 * de historial sobre la asociacion, con su propio {@code estado}.</p>
 *
 * <p>Al igual que {@link SensorColmena}, solo puede existir UN
 * monitoreo activo por asociacion a la vez (impuesto por la columna
 * generada {@code activo_uniq} + indice unico).</p>
 *
 * <p><b>Reglas de negocio pendientes de {@code service} (hoy en
 * triggers):</b> esta es la entidad con la validacion cruzada mas
 * exigente del esquema. Antes de permitir crear o actualizar un
 * {@code Monitoreo} con {@code estado = "activo"}, el
 * {@code service} debera verificar (replicando
 * {@code trg_monitoreo_ins/upd} y {@code trg_monitoreo_empresa_bi/bu}):</p>
 * <ul>
 *     <li>La {@link SensorColmena} referenciada esta {@code activo = true}.</li>
 *     <li>El {@link Sensor} de esa asociacion esta en {@code estado = "activo"}.</li>
 *     <li>La {@link Colmena} de esa asociacion esta en {@code estado = "Estable"}.</li>
 * </ul>
 * <p>Si cualquiera de las tres condiciones falla, la base de datos
 * rechazaria la operacion de todas formas (el trigger sigue activo),
 * pero sin esta validacion en el service el usuario final recibiria un
 * error generico de base de datos en vez de un mensaje de negocio
 * claro -- por eso se documenta aqui como pendiente explicito, no como
 * algo ya resuelto.</p>
 */
@Entity
@Table(name = "monitoreo")
public class Monitoreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_monitoreo")
    private Integer idMonitoreo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_asociacion", nullable = false)
    private SensorColmena asociacion;

    /**
     * Estado del monitoreo (por ejemplo "activo"). Texto libre, igual
     * que otros campos {@code estado} del proyecto, porque asi esta
     * definido en SQL (VARCHAR, no ENUM).
     */
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_inicio", insertable = false, updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    /**
     * Columna generada por MySQL, de solo lectura -- ver la explicacion
     * equivalente en {@link SensorColmena#getActivoUniq()} sobre por
     * que se usa {@code Byte} y no {@code Integer} (la columna real es
     * {@code TINYINT}, no {@code INT}).
     */
    @Column(name = "activo_uniq", insertable = false, updatable = false)
    private Byte activoUniq;

    /** Constructor vacio requerido por JPA. */
    protected Monitoreo() {
    }

    public Monitoreo(SensorColmena asociacion, String estado) {
        this.asociacion = asociacion;
        this.estado = estado;
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Integer getIdMonitoreo() {
        return idMonitoreo;
    }

    public SensorColmena getAsociacion() {
        return asociacion;
    }

    public void setAsociacion(SensorColmena asociacion) {
        this.asociacion = asociacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    public Byte getActivoUniq() {
        return activoUniq;
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Monitoreo other)) return false;
        return idMonitoreo != null && idMonitoreo.equals(other.idMonitoreo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    @Override
    public String toString() {
        return "Monitoreo{" +
                "idMonitoreo=" + idMonitoreo +
                ", estado='" + estado + '\'' +
                '}';
    }
}
