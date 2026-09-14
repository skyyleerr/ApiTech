package com.apitech.mk5.entity.empresa;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad JPA que mapea la tabla {@code empresas}.
 *
 * <p>Representa a un cliente (tenant) de ApiTech: una organizacion
 * apicola que contrata la plataforma. Es la raiz de la jerarquia
 * multi-tenant del sistema -- casi todas las demas tablas del esquema
 * (usuarios, colmenas, sensores, suscripciones, etc.) referencian a una
 * empresa, directa o indirectamente.</p>
 *
 * <p><b>Decision de mapeo relevante:</b> las columnas {@code fecha_registro}
 * y {@code fecha_actualizacion} tienen valores por defecto gestionados por
 * MySQL ({@code DEFAULT CURRENT_TIMESTAMP} y {@code ON UPDATE CURRENT_TIMESTAMP}
 * respectivamente). Por eso se mapean como {@code insertable = false,
 * updatable = false}: Hibernate nunca intenta escribir un valor en esas
 * columnas, dejando que sea la propia base de datos quien las controle.
 * El valor se lee normalmente al consultar la fila.</p>
 */
@Entity
@Table(name = "empresas")
public class Empresa {

    /**
     * Clave primaria autogenerada por MySQL (AUTO_INCREMENT).
     *
     * <p>{@code GenerationType.IDENTITY} le indica a Hibernate que no debe
     * generar el id por su cuenta (por ejemplo con una secuencia), sino
     * dejar que la propia base de datos lo asigne al insertar la fila y
     * luego recuperarlo.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Integer idEmpresa;

    @Column(name = "nit", nullable = false, unique = true, length = 20)
    private String nit;

    @Column(name = "nombre_empresa", nullable = false, length = 150)
    private String nombreEmpresa;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "direccion", length = 200)
    private String direccion;

    /**
     * Estado de la empresa dentro del sistema (por ejemplo: "activa",
     * "inactiva"). Se mapea como texto libre porque asi esta definido en
     * el esquema SQL (VARCHAR, no ENUM) -- no se reinterpreta como un
     * enum de Java para no introducir una restriccion que la base de
     * datos no tiene.
     */
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    /**
     * Constructor vacio requerido por la especificacion JPA: Hibernate
     * necesita poder instanciar la entidad por reflexion (sin argumentos)
     * antes de rellenar sus campos al leer una fila de la base de datos.
     */
    protected Empresa() {
    }

    /**
     * Constructor de uso en el codigo de la aplicacion (por ejemplo,
     * desde el service al crear una empresa nueva). El id no se recibe
     * aqui porque lo asigna la base de datos al insertar.
     */
    public Empresa(String nit, String nombreEmpresa, String correo,
                    String telefono, String direccion, String estado) {
        this.nit = nit;
        this.nombreEmpresa = nombreEmpresa;
        this.correo = correo;
        this.telefono = telefono;
        this.direccion = direccion;
        this.estado = estado;
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Integer getIdEmpresa() {
        return idEmpresa;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getNombreEmpresa() {
        return nombreEmpresa;
    }

    public void setNombreEmpresa(String nombreEmpresa) {
        this.nombreEmpresa = nombreEmpresa;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
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

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    /**
     * Dos objetos {@code Empresa} se consideran iguales si representan la
     * misma fila (mismo {@code idEmpresa}), no si todos sus atributos
     * coinciden por valor. Esto es la nocion de "identidad" propia de
     * una entidad JPA -- distinta de comparar dos referencias con
     * {@code ==}, que solo seria verdadero si ambas variables apuntan al
     * mismo objeto en el heap de la JVM.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Empresa other)) return false;
        return idEmpresa != null && idEmpresa.equals(other.idEmpresa);
    }

    /**
     * Se usa un hashCode constante (no basado en idEmpresa) a proposito:
     * si el hashCode dependiera del id, cambiaria en el momento en que la
     * entidad pasa de "transitoria" (sin id, antes de guardarse) a
     * "persistida" (con id ya asignado por la base de datos), lo cual
     * romperia su ubicacion dentro de colecciones como HashSet o HashMap.
     * Es una practica recomendada al trabajar con entidades JPA.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    @Override
    public String toString() {
        return "Empresa{" +
                "idEmpresa=" + idEmpresa +
                ", nit='" + nit + '\'' +
                ", nombreEmpresa='" + nombreEmpresa + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}
