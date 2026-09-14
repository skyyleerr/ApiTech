package com.apitech.mk5.dto.request;

import com.apitech.mk5.entity.auditoria.ActorTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de entrada de un registro de auditoria. */
public record AuditoriaRequestDTO(
        Integer idUsuario,
        @NotNull(message = "El tipo de actor es obligatorio")
        ActorTipo actorTipo,
        @NotBlank(message = "La accion es obligatoria")
        @Size(max = 100, message = "La accion no puede superar 100 caracteres")
        String accion,
        @Size(max = 50, message = "El modulo no puede superar 50 caracteres")
        String modulo,
        @Size(max = 50, message = "El tipo de entidad no puede superar 50 caracteres")
        String entidadTipo,
        Long idEntidad,
        @Size(max = 100, message = "El registro afectado no puede superar 100 caracteres")
        String registroAfectado,
        String descripcion
) {
}
