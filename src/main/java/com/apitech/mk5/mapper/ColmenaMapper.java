package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.request.ColmenaRequestDTO;
import com.apitech.mk5.dto.response.ColmenaResponseDTO;
import com.apitech.mk5.entity.apiario.Colmena;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.usuario.Usuario;

public final class ColmenaMapper {

    private ColmenaMapper() {
    }

    public static Colmena toEntity(ColmenaRequestDTO dto, Empresa empresa, Usuario usuarioRegistro) {
        return new Colmena(empresa, usuarioRegistro, dto.nombre(), dto.ubicacion(), dto.estado());
    }

    public static void aplicarCambios(Colmena entidad, ColmenaRequestDTO dto, Empresa empresa, Usuario usuarioRegistro) {
        entidad.setEmpresa(empresa);
        entidad.setUsuarioRegistro(usuarioRegistro);
        entidad.setNombre(dto.nombre());
        entidad.setUbicacion(dto.ubicacion());
        entidad.setEstado(dto.estado());
    }

    public static ColmenaResponseDTO toResponseDTO(Colmena colmena) {
        return new ColmenaResponseDTO(
                colmena.getIdColmena(),
                EmpresaMapper.toResponseDTO(colmena.getEmpresa()),
                UsuarioResumenMapper.toResumenDTO(colmena.getUsuarioRegistro()),
                colmena.getNombre(),
                colmena.getUbicacion(),
                colmena.getEstado(),
                colmena.getFechaCreacion(),
                colmena.getFechaActualizacion()
        );
    }
}
