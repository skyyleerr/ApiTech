package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.request.AuditoriaRequestDTO;
import com.apitech.mk5.dto.response.AuditoriaResponseDTO;
import com.apitech.mk5.entity.auditoria.Auditoria;
import com.apitech.mk5.entity.usuario.Usuario;

public final class AuditoriaMapper {
    private AuditoriaMapper() {
    }

    public static Auditoria toEntity(AuditoriaRequestDTO dto, Usuario usuario) {
        return new Auditoria(usuario, dto.actorTipo(), dto.accion(), dto.modulo(),
                dto.entidadTipo(), dto.idEntidad(), dto.registroAfectado(), dto.descripcion());
    }

    public static void aplicarCambios(Auditoria entidad, AuditoriaRequestDTO dto,
                                      Usuario usuario) {
        entidad.setUsuario(usuario);
        entidad.setActorTipo(dto.actorTipo());
        entidad.setAccion(dto.accion());
        entidad.setModulo(dto.modulo());
        entidad.setEntidadTipo(dto.entidadTipo());
        entidad.setIdEntidad(dto.idEntidad());
        entidad.setRegistroAfectado(dto.registroAfectado());
        entidad.setDescripcion(dto.descripcion());
    }

    public static AuditoriaResponseDTO toResponseDTO(Auditoria entidad) {
        return new AuditoriaResponseDTO(
                entidad.getIdAuditoria(),
                entidad.getUsuario() != null ? entidad.getUsuario().getIdUsuario() : null,
                entidad.getActorTipo(), entidad.getAccion(), entidad.getModulo(),
                entidad.getEntidadTipo(), entidad.getIdEntidad(), entidad.getRegistroAfectado(),
                entidad.getDescripcion(), entidad.getFecha()
        );
    }
}
