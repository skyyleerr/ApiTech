package com.apitech.mk5.repository.usuario;

import com.apitech.mk5.entity.usuario.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link Permiso}.
 */
public interface PermisoRepository extends JpaRepository<Permiso, Integer> {

    /**
     * Busca un permiso por su codigo exacto (por ejemplo,
     * "GESTIONAR_USUARIOS").
     *
     * @param codigoPermiso codigo del permiso tal como esta registrado
     *                      en la tabla {@code permisos}.
     * @return el permiso encontrado, o {@link Optional#empty()} si no existe.
     */
    Optional<Permiso> findByCodigoPermiso(String codigoPermiso);

}
