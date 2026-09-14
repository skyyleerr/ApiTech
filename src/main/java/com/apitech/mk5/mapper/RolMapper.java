package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.response.RolResponseDTO;
import com.apitech.mk5.entity.usuario.Rol;

/**
 * Conversion de {@link Rol} a su DTO de salida (no hay DTO de entrada,
 * ver la nota en {@link RolResponseDTO}).
 */
public final class RolMapper {

    private RolMapper() {
    }

    public static RolResponseDTO toResponseDTO(Rol rol) {
        return new RolResponseDTO(rol.getIdRol(), rol.getNombreRol(), rol.getDescripcion());
    }
}
