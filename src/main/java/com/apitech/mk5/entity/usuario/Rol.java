package com.apitech.mk5.entity.usuario;

import jakarta.persistence.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entidad JPA que mapea la tabla {@code roles}.
 *
 * <p>Catalogo de roles del sistema RBAC (control de acceso basado en
 * roles). Los nombres esperados por las reglas de negocio del esquema
 * (ver los triggers {@code trg_usuarios_empresa_ins/upd} de la base de
 * datos) son: {@code Admin_ApiTech}, {@code Empleado_ApiTech},
 * {@code Admin_Cliente} y {@code Empleado_Cliente}. Estos valores viven
 * como FILAS de esta tabla (datos), no como constantes fijas en el
 * codigo Java -- el catalogo se administra en base de datos.</p>
 *
 * <p>Nota: "Sistema" (el actor automatico usado en auditoria) NO es un
 * rol de esta tabla; es un valor del enum {@code actor_tipo} de la
 * tabla {@code auditoria}, nunca un rol asignable a un usuario que
 * inicia sesion. Esta entidad se mapeara mas adelante, cuando se
 * construya el modulo de auditoria.</p>
 *
 * <p><b>Relacion N:M con {@link Permiso}:</b> se mapea a traves de la
 * tabla intermedia {@code roles_permisos}, que en el esquema SQL no
 * tiene columnas propias mas alla de las dos claves foraneas (por eso
 * NO se crea una entidad Java para {@code roles_permisos} -- alcanza
 * con {@code @ManyToMany} + {@code @JoinTable}). {@code Rol} es el lado
 * "dueno" de la relacion (el que declara el {@code @JoinTable} y por lo
 * tanto el que controla los INSERT/DELETE sobre la tabla intermedia
 * cuando se guarda un {@code Rol}); {@link Permiso} solo tiene el lado
 * inverso, de solo lectura, para poder navegar "que roles tienen este
 * permiso" sin duplicar el control de la relacion en los dos lados.</p>
 */
@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer idRol;

    @Column(name = "nombre_rol", nullable = false, unique = true, length = 50)
    private String nombreRol;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    /**
     * Permisos asociados a este rol (tabla intermedia {@code roles_permisos}).
     *
     * <p>Se usa {@link java.util.Set} y no {@link java.util.List} a
     * proposito: en una relacion N:M no tiene sentido que el mismo
     * permiso aparezca repetido para un rol, y {@code Set} expresa esa
     * regla directamente en el tipo, sin depender de que quien use la
     * clase recuerde no duplicar elementos.</p>
     *
     * <p>{@code fetch = FetchType.LAZY} (el valor por defecto de
     * Hibernate para colecciones, pero se deja explicito para que sea
     * evidente): los permisos NO se cargan desde la base de datos junto
     * con el {@code Rol}, sino solo si alguien llama a
     * {@link #getPermisos()} y realmente los recorre. Evita traer datos
     * que no se van a usar en la mayoria de los casos (por ejemplo, al
     * listar usuarios normalmente no hace falta ver los permisos de su
     * rol).</p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "roles_permisos",
            joinColumns = @JoinColumn(name = "id_rol"),
            inverseJoinColumns = @JoinColumn(name = "id_permiso")
    )
    private Set<Permiso> permisos = new HashSet<>();

    /** Constructor vacio requerido por JPA. */
    protected Rol() {
    }

    public Rol(String nombreRol, String descripcion) {
        this.nombreRol = nombreRol;
        this.descripcion = descripcion;
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Integer getIdRol() {
        return idRol;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve una vista de SOLO LECTURA de los permisos de este rol.
     *
     * <p>No se expone el {@code Set} interno directamente ni se ofrece
     * un {@code setPermisos(...)}: si se permitiera reemplazar toda la
     * coleccion de una vez, seria facil perder permisos por accidente
     * (por ejemplo, al mapear un DTO mal construido). En su lugar, la
     * unica forma de modificar la relacion es a traves de
     * {@link #addPermiso(Permiso)} y {@link #removePermiso(Permiso)},
     * que dejan explicito en el codigo que se esta agregando o quitando
     * UN permiso a la vez.</p>
     *
     * @return coleccion inmutable de los permisos actuales del rol.
     */
    public Set<Permiso> getPermisos() {
        return Collections.unmodifiableSet(permisos);
    }

    /**
     * Asocia un permiso a este rol (equivalente a un INSERT en
     * {@code roles_permisos} al guardar el rol).
     *
     * @param permiso permiso a asociar; no debe ser {@code null}.
     */
    public void addPermiso(Permiso permiso) {
        this.permisos.add(permiso);
    }

    /**
     * Retira un permiso de este rol (equivalente a un DELETE en
     * {@code roles_permisos} al guardar el rol).
     *
     * @param permiso permiso a retirar.
     */
    public void removePermiso(Permiso permiso) {
        this.permisos.remove(permiso);
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rol other)) return false;
        return idRol != null && idRol.equals(other.idRol);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    @Override
    public String toString() {
        return "Rol{" +
                "idRol=" + idRol +
                ", nombreRol='" + nombreRol + '\'' +
                '}';
    }
}
