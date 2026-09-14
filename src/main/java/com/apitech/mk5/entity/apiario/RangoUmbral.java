package com.apitech.mk5.entity.apiario;

import com.apitech.mk5.entity.empresa.Empresa;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad JPA que mapea la tabla {@code rangos_umbrales}.
 *
 * <p>Define, para un tipo de medicion, el rango de valores considerados
 * normales (fuera de ese rango se dispara una {@code Alerta}, en una
 * etapa posterior). Puede ser especifico de una empresa
 * ({@code empresa != null}) o un umbral global por defecto de ApiTech
 * ({@code empresa == null}).</p>
 *
 * <p><b>Regla de resolucion (documentada en el SQL, NO implementada
 * todavia -- pendiente de {@code service}):</b> para saber que umbral
 * aplica a una empresa y tipo de medicion dados, primero se debe buscar
 * un umbral con {@code empresa} igual a esa empresa; si no existe
 * ninguno, se usa el umbral global ({@code empresa IS NULL}) para ese
 * mismo tipo. Esta prioridad NO se resuelve en SQL ni en esta entidad
 * -- sera un metodo dedicado en {@code RangoUmbralService}
 * (por ejemplo, {@code resolverUmbralAplicable(empresa, tipoMedicion)})
 * que intente primero {@link RangoUmbral} especifico y haga una
 * segunda consulta al global solo si la primera no encuentra nada.</p>
 *
 * <p><b>Regla de validacion pendiente (hoy en los triggers
 * {@code trg_umbral_bi/bu}):</b> {@code valorMin} debe ser
 * estrictamente menor que {@code valorMax}. No se puede expresar como
 * una anotacion de Bean Validation sobre un solo campo (compara dos
 * campos entre si) -- se implementara como un validador a nivel de
 * clase en el paquete {@code validation}, o como chequeo explicito en
 * el {@code service}, cuando se construya esa capa.</p>
 */
@Entity
@Table(name = "rangos_umbrales")
public class RangoUmbral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_umbral")
    private Integer idUmbral;

    /**
     * {@code null} significa "umbral global por defecto de ApiTech",
     * no un dato faltante -- es un valor de negocio valido y esperado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = true)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_medicion", nullable = false, length = 20)
    private TipoMedicion tipoMedicion;

    @Column(name = "valor_min", nullable = false)
    private float valorMin;

    @Column(name = "valor_max", nullable = false)
    private float valorMax;

    @Column(name = "unidad", nullable = false, length = 10)
    private String unidad;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_configuracion", insertable = false, updatable = false)
    private LocalDateTime fechaConfiguracion;

    /**
     * Columna generada por MySQL: {@code IFNULL(id_empresa, 0)}. Existe
     * SOLO para poder declarar un indice unico que evite dos umbrales
     * globales para el mismo tipo de medicion (en SQL, {@code NULL !=
     * NULL}, asi que sin esta normalizacion el UNIQUE original no
     * bloquearia duplicados globales). Es {@code Integer} (no
     * {@code Byte}) porque a diferencia de {@code activo_uniq} en
     * {@code sensor_colmena}/{@code monitoreo}, esta columna generada
     * es {@code INT}, no {@code TINYINT} -- se verifico el tipo exacto
     * en el script SQL antes de mapearla, precisamente para no repetir
     * el error de tipo de la etapa anterior.
     */
    @Column(name = "id_empresa_norm", insertable = false, updatable = false)
    private Integer idEmpresaNorm;

    /** Constructor vacio requerido por JPA. */
    protected RangoUmbral() {
    }

    /**
     * @param empresa {@code null} para crear un umbral global.
     */
    public RangoUmbral(Empresa empresa, TipoMedicion tipoMedicion, float valorMin,
                        float valorMax, String unidad, String estado) {
        this.empresa = empresa;
        this.tipoMedicion = tipoMedicion;
        this.valorMin = valorMin;
        this.valorMax = valorMax;
        this.unidad = unidad;
        this.estado = estado;
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Integer getIdUmbral() {
        return idUmbral;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public TipoMedicion getTipoMedicion() {
        return tipoMedicion;
    }

    public void setTipoMedicion(TipoMedicion tipoMedicion) {
        this.tipoMedicion = tipoMedicion;
    }

    public float getValorMin() {
        return valorMin;
    }

    public void setValorMin(float valorMin) {
        this.valorMin = valorMin;
    }

    public float getValorMax() {
        return valorMax;
    }

    public void setValorMax(float valorMax) {
        this.valorMax = valorMax;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaConfiguracion() {
        return fechaConfiguracion;
    }

    public Integer getIdEmpresaNorm() {
        return idEmpresaNorm;
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RangoUmbral other)) return false;
        return idUmbral != null && idUmbral.equals(other.idUmbral);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    @Override
    public String toString() {
        return "RangoUmbral{" +
                "idUmbral=" + idUmbral +
                ", tipoMedicion=" + tipoMedicion +
                ", valorMin=" + valorMin +
                ", valorMax=" + valorMax +
                '}';
    }
}
