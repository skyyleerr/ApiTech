package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.response.UsuarioResumenDTO;
import com.apitech.mk5.entity.usuario.Usuario;

public final class UsuarioResumenMapper {

    private UsuarioResumenMapper() {
    }

    public static UsuarioResumenDTO toResumenDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResumenDTO(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo()
        );
    }
}
