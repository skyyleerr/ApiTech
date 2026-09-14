package com.apitech.mk5.entity.usuario;

import com.apitech.mk5.entity.empresa.Empresa;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad JPA que mapea la tabla {@code usuarios}.
 *
 * <p>Representa a cualquier persona que inicia sesion en ApiTech: tanto
 * el personal de la propia ApiTech ({@code Admin_ApiTech},
 * {@code Empleado_ApiTech}, sin empresa asociada) como el personal de
 * una empresa cliente ({@code Admin_Cliente}, {@code Empleado_Cliente},
 * siempre con empresa asociada).</p>
 *
 * <p><b>Regla de negocio importante (NO implementada aqui todavia):</b>
 * el esquema SQL fuerza con triggers ({@code trg_usuarios_empresa_ins/upd})
 * que un usuario ApiTech nunca tenga {@code empresa} asignada, y que un
 * usuario cliente siempre la tenga. Esta entidad solo declara que
 * {@code empresa} PUEDE ser nula a nivel de columna (coincidiendo con
 * el {@code NULL} permitido en la tabla) -- la regla cruzada que decide
 * CUANDO debe o no debe ser nula, segun el rol, se implementara en la
 * capa de {@code service} en una etapa posterior (es el equivalente en
 * Java a esos triggers). Intentar guardar un usuario con una
 * combinacion invalida hoy fallaria igualmente, pero por el lado de la
 * base de datos (el trigger seguiria activo), no con un mensaje de
 * negocio claro desde la aplicacion.</p>
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    /**
     * Empresa a la que pertenece el usuario. {@code NULL} unicamente
     * para usuarios de ApiTech (Admin_ApiTech / Empleado_ApiTech).
     *
     * <p>{@code @ManyToOne} es el lado "muchos" de la relacion
     * Empresa 1:N Usuario -- muchos usuarios pueden apuntar a la misma
     * empresa. {@code fetch = FetchType.LAZY} evita que, cada vez que
     * se cargue un usuario desde la base de datos, Hibernate traiga
     * tambien automaticamente los datos completos de su empresa; solo
     * se cargan si el codigo realmente llama a {@link #getEmpresa()} y
     * accede a sus campos.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = true)
    private Empresa empresa;

    /**
     * Rol del usuario dentro del sistema RBAC. A diferencia de
     * {@code empresa}, este campo es obligatorio ({@code nullable =
     * false}), reflejando que la columna {@code id_rol} de la tabla es
     * {@code NOT NULL}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", length = 100)
    private String apellido;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String correo;

    /**
     * Hash de la contrasena (bcrypt), nunca la contrasena en texto
     * plano. La generacion del hash es responsabilidad de la capa de
     * seguridad/servicio, no de esta entidad -- aqui solo se almacena
     * el resultado.
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    /** Constructor vacio requerido por JPA. */
    protected Usuario() {
    }

    /**
     * Constructor de uso en la aplicacion. {@code empresa} puede
     * recibirse como {@code null} (usuarios ApiTech); el id no se
     * recibe porque lo asigna la base de datos.
     */
    public Usuario(Empresa empresa, Rol rol, String nombre, String apellido,
                    String correo, String password, String estado) {
        this.empresa = empresa;
        this.rol = rol;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.password = password;
        this.estado = estado;
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public LocalDateTime getUltimoLogin() {
        return ultimoLogin;
    }

    public void setUltimoLogin(LocalDateTime ultimoLogin) {
        this.ultimoLogin = ultimoLogin;
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario other)) return false;
        return idUsuario != null && idUsuario.equals(other.idUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    /**
     * OJO: {@code toString()} deliberadamente NO incluye {@code password}
     * (ni siquiera el hash), para que nunca aparezca en un log por
     * accidente si alguien imprime un {@code Usuario} durante una
     * depuracion.
     */
    @Override
    public String toString() {
        return "Usuario{" +
                "idUsuario=" + idUsuario +
                ", correo='" + correo + '\'' +
                ", nombre='" + nombre + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}
