package com.apitech.mk5.dto.response;

import com.apitech.mk5.entity.apiario.OrigenMedicion;
import com.apitech.mk5.entity.apiario.TipoMedicion;

import java.time.LocalDateTime;

public record MedicionResponseDTO(
        Long idMedicion,
        Integer idAsociacion,
        TipoMedicion tipoMedicion,
        float valor,
        String unidad,
        OrigenMedicion origen,
        LocalDateTime fecha
) {
}
