package com.apitech.mk5.entity.usuario;

import jakarta.persistence.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entidad JPA que mapea la tabla {@code permisos}.
 *
 * <p>Catalogo de permisos individuales del sistema RBAC. Cada rol
 * ({@link Rol}) se asocia a uno o varios permisos a traves de la tabla
 * intermedia {@code roles_permisos} (relacion N:M, sin atributos propios
 * en esa tabla intermedia mas alla de las dos claves foraneas).</p>
 *
 * <p>Esta clase es el lado INVERSO de la relacion ({@code mappedBy}):
 * {@link Rol} es quien controla la relacion (declara el
 * {@code @JoinTable} y decide cuando se inserta/borra en
 * {@code roles_permisos}). Por eso aqui solo existe un getter de solo
 * lectura para navegar "que roles usan este permiso" -- para asociar o
 * quitar un permiso de un rol, se usa {@link Rol#addPermiso(Permiso)} /
 * {@link Rol#removePermiso(Permiso)}, nunca esta clase.</p>
 */
@Entity
@Table(name = "permisos")
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Integer idPermiso;

    @Column(name = "codigo_permiso", nullable = false, unique = true, length = 60)
    private String codigoPermiso;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    /**
     * Lado inverso de la relacion N:M -- {@code mappedBy = "permisos"}
     * indica que la columna de union ya esta definida del lado de
     * {@link Rol#getPermisos()}; aqui no se repite el {@code @JoinTable}.
     */
    @ManyToMany(mappedBy = "permisos", fetch = FetchType.LAZY)
    private Set<Rol> roles = new HashSet<>();

    /** Constructor vacio requerido por JPA. */
    protected Permiso() {
    }

    public Permiso(String codigoPermiso, String descripcion) {
        this.codigoPermiso = codigoPermiso;
        this.descripcion = descripcion;
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Integer getIdPermiso() {
        return idPermiso;
    }

    public String getCodigoPermiso() {
        return codigoPermiso;
    }

    public void setCodigoPermiso(String codigoPermiso) {
        this.codigoPermiso = codigoPermiso;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve una vista de solo lectura de los roles que tienen
     * asociado este permiso. Para modificar la relacion, usar
     * {@link Rol#addPermiso(Permiso)} / {@link Rol#removePermiso(Permiso)}.
     */
    public Set<Rol> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permiso other)) return false;
        return idPermiso != null && idPermiso.equals(other.idPermiso);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    @Override
    public String toString() {
        return "Permiso{" +
                "idPermiso=" + idPermiso +
                ", codigoPermiso='" + codigoPermiso + '\'' +
                '}';
    }
}
