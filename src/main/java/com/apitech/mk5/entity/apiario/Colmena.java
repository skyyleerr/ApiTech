package com.apitech.mk5.entity.apiario;

import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.usuario.Usuario;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad JPA que mapea la tabla {@code colmenas}.
 *
 * <p>Es el activo principal del dominio apicola: cada colmena
 * pertenece a exactamente una empresa, y opcionalmente registra quien
 * (que {@link Usuario}) la dio de alta -- ese dato es solo informativo,
 * no afecta quien puede ver o gestionar la colmena (eso se resuelve
 * siempre por {@code empresa}, nunca por {@code usuarioRegistro}; ver
 * la nota sobre la inconsistencia detectada en MK3.5 en el analisis
 * previo del proyecto).</p>
 *
 * <p><b>Sobre la clave unica compuesta {@code uq_colmena_empresa
 * (id_colmena, id_empresa)}</b> que existe en el SQL: no se mapea aqui
 * como una relacion JPA adicional. Existe unicamente para que otras
 * tablas (como {@code sensor_colmena}) puedan declarar una FK compuesta
 * hacia {@code (id_colmena, id_empresa)} y asi la propia base de datos
 * garantice que un sensor y una colmena asociados pertenezcan a la
 * misma empresa. Es un mecanismo de integridad a nivel de base de
 * datos, no una relacion conceptual distinta -- por eso en Java el
 * campo {@code idEmpresa} de esas tablas hijas se tratara como una
 * columna simple, con la coherencia validada explicitamente en el
 * {@code service} correspondiente (ver arquitectura acordada).</p>
 */
@Entity
@Table(name = "colmenas")
public class Colmena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_colmena")
    private Integer idColmena;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    /**
     * Usuario que registro la colmena. Puede ser {@code null} (por
     * ejemplo, si el usuario que la creo fue eliminado despues -- la
     * columna tiene {@code ON DELETE SET NULL} en el esquema SQL, lo
     * que confirma que este dato es prescindible, solo historico).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = true)
    private Usuario usuarioRegistro;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "ubicacion", length = 150)
    private String ubicacion;

    /**
     * Estado operativo de la colmena (por ejemplo "Estable"). Se
     * mantiene como {@code String} y no como {@code enum} porque en el
     * esquema SQL esta columna es {@code VARCHAR(50)}, no un
     * {@code ENUM} -- a diferencia de {@link Sensor#getTipo()}, aqui la
     * base de datos no restringe los valores posibles, asi que Java
     * tampoco deberia inventar una restriccion que la base de datos no
     * tiene.
     */
    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    /** Constructor vacio requerido por JPA. */
    protected Colmena() {
    }

    /**
     * Constructor de uso en la aplicacion. {@code usuarioRegistro}
     * puede ser {@code null} si no se quiere registrar quien la creo.
     */
    public Colmena(Empresa empresa, Usuario usuarioRegistro, String nombre,
                    String ubicacion, String estado) {
        this.empresa = empresa;
        this.usuarioRegistro = usuarioRegistro;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.estado = estado;
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Integer getIdColmena() {
        return idColmena;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Usuario usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Colmena other)) return false;
        return idColmena != null && idColmena.equals(other.idColmena);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    @Override
    public String toString() {
        return "Colmena{" +
                "idColmena=" + idColmena +
                ", nombre='" + nombre + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}
