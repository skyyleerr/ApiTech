package com.apitech.mk5.entity.apiario;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad JPA que mapea la tabla {@code mediciones}.
 *
 * <p>Historial de lecturas de sensores -- la tabla con mayor volumen
 * esperado de todo el sistema (por eso su clave primaria es
 * {@code BIGINT} en SQL, mapeada aqui como {@code Long}, no
 * {@code Integer}: con millones de filas acumuladas por anios, un
 * {@code INT} de 32 bits eventualmente se quedaria sin rango).</p>
 *
 * <p><b>Decision de diseno importante para el repository:</b> por ser
 * la tabla de mayor volumen, NINGUN metodo de consulta sobre
 * {@code Medicion} debe devolver una lista sin acotar. Todos los
 * metodos de listado en {@link com.apitech.mk5.repository.apiario.MedicionRepository}
 * usan {@code Pageable}/{@code Page} en vez de {@code List}, para que
 * quien consuma este repository (incluyendo el futuro modulo de
 * reportes) nunca pueda pedir "todas las mediciones de un sensor" de
 * una sola vez por accidente.</p>
 *
 * <p><b>Regla de negocio pendiente de {@code service} (hoy en
 * {@code trg_mediciones_coherencia_bi/bu}):</b> antes de guardar una
 * medicion, el service debera verificar que (1) la asociacion
 * referenciada tenga un {@link Monitoreo} activo, (2)
 * {@code tipoMedicion} coincida con el tipo del {@link Sensor} de esa
 * asociacion, y (3) {@code origen} coincida con
 * {@link Sensor#isEsSimulado()} (simulado con SIMULADO, fisico con REAL).</p>
 */
@Entity
@Table(name = "mediciones")
public class Medicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medicion")
    private Long idMedicion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_asociacion", nullable = false)
    private SensorColmena asociacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_medicion", nullable = false, length = 20)
    private TipoMedicion tipoMedicion;

    @Column(name = "valor", nullable = false)
    private float valor;

    @Column(name = "unidad", nullable = false, length = 10)
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 10)
    private OrigenMedicion origen;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    /** Constructor vacio requerido por JPA. */
    protected Medicion() {
    }

    public Medicion(SensorColmena asociacion, TipoMedicion tipoMedicion, float valor,
                     String unidad, OrigenMedicion origen) {
        this.asociacion = asociacion;
        this.tipoMedicion = tipoMedicion;
        this.valor = valor;
        this.unidad = unidad;
        this.origen = origen;
        this.fecha = LocalDateTime.now();
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Long getIdMedicion() {
        return idMedicion;
    }

    public SensorColmena getAsociacion() {
        return asociacion;
    }

    public void setAsociacion(SensorColmena asociacion) {
        this.asociacion = asociacion;
    }

    public TipoMedicion getTipoMedicion() {
        return tipoMedicion;
    }

    public void setTipoMedicion(TipoMedicion tipoMedicion) {
        this.tipoMedicion = tipoMedicion;
    }

    public float getValor() {
        return valor;
    }

    public void setValor(float valor) {
        this.valor = valor;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public OrigenMedicion getOrigen() {
        return origen;
    }

    public void setOrigen(OrigenMedicion origen) {
        this.origen = origen;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medicion other)) return false;
        return idMedicion != null && idMedicion.equals(other.idMedicion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    @Override
    public String toString() {
        return "Medicion{" +
                "idMedicion=" + idMedicion +
                ", tipoMedicion=" + tipoMedicion +
                ", valor=" + valor +
                ", origen=" + origen +
                '}';
    }
}
