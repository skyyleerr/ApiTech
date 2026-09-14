package com.apitech.mk5.dto.response;

import java.time.LocalDateTime;

public record MonitoreoResponseDTO(
        Integer idMonitoreo,
        Integer idAsociacion,
        String estado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin
) {
}
