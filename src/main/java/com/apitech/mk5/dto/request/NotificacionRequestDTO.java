package com.apitech.mk5.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de entrada; la empresa se deriva del usuario y de las referencias. */
public record NotificacionRequestDTO(
        @NotNull(message = "El usuario es obligatorio")
        Integer idUsuario,
        @NotBlank(message = "El tipo es obligatorio")
        @Size(max = 50, message = "El tipo no puede superar 50 caracteres")
        String tipo,
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 150, message = "El titulo no puede superar 150 caracteres")
        String titulo,
        String mensaje,
        @NotNull(message = "Debe indicarse si la notificacion fue leida")
        Boolean leida,
        Integer idAlerta,
        Integer idPago
) {
}
