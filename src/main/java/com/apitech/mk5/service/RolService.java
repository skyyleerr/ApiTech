package com.apitech.mk5.service;

import com.apitech.mk5.dto.response.RolResponseDTO;

import java.util.List;

/**
 * Consulta del catalogo de roles. No hay operaciones de creacion/edicion
 * expuestas: los roles son un catalogo fijo del sistema (ver la nota en
 * {@link com.apitech.mk5.dto.response.RolResponseDTO}).
 */
public interface RolService {

    List<RolResponseDTO> listarTodos();

    /**
     * @throws com.apitech.mk5.exception.RecursoNoEncontradoException
     *         si no existe un rol con ese id.
     */
    RolResponseDTO obtenerPorId(Integer id);

}
