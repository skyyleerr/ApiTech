package com.apitech.mk5.dto.response;

import com.apitech.mk5.entity.auditoria.ActorTipo;

import java.time.LocalDateTime;

public record AuditoriaResponseDTO(
        Integer idAuditoria,
        Integer idUsuario,
        ActorTipo actorTipo,
        String accion,
        String modulo,
        String entidadTipo,
        Long idEntidad,
        String registroAfectado,
        String descripcion,
        LocalDateTime fecha
) {
}
