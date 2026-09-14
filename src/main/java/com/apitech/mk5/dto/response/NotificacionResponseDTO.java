package com.apitech.mk5.dto.response;

import java.time.LocalDateTime;

public record NotificacionResponseDTO(
        Integer idNotificacion,
        Integer idUsuario,
        Integer idEmpresa,
        String tipo,
        String titulo,
        String mensaje,
        boolean leida,
        LocalDateTime fecha,
        Integer idAlerta,
        Integer idPago
) {
}
