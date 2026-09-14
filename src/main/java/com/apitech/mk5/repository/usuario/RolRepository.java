package com.apitech.mk5.repository.usuario;

import com.apitech.mk5.entity.usuario.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link Rol}.
 *
 * <p>Ademas del CRUD heredado de {@code JpaRepository}, expone la
 * busqueda por nombre de rol, que sera necesaria en varios puntos del
 * dominio: por ejemplo, para resolver el rol "Admin_Cliente" al crear el
 * primer usuario administrador de una empresa nueva, o para aplicar la
 * regla de coherencia rol-empresa que hoy vive en los triggers
 * {@code trg_usuarios_empresa_ins/upd} de la base de datos.</p>
 */
public interface RolRepository extends JpaRepository<Rol, Integer> {

    /**
     * Busca un rol por su nombre exacto (por ejemplo, "Admin_Cliente").
     *
     * @param nombreRol nombre del rol tal como esta registrado en la
     *                   tabla {@code roles}.
     * @return el rol encontrado, o {@link Optional#empty()} si no existe.
     */
    Optional<Rol> findByNombreRol(String nombreRol);

    List<Rol> findByNombreRolInOrderByNombreRolAsc(java.util.Collection<String> nombresRol);

}
