package com.apitech.mk5.dto.response;

import java.time.LocalDateTime;

public record SensorColmenaResponseDTO(
        Integer idAsociacion,
        SensorResponseDTO sensor,
        ColmenaResponseDTO colmena,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        boolean activo
) {
}
