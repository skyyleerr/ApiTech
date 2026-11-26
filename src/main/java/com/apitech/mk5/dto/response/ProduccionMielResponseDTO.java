package com.apitech.mk5.dto.response;

import java.time.LocalDate;

public record ProduccionMielResponseDTO(
        Integer idProduccion,
        Integer idColmena,
        Integer idEmpresa,
        Float cantidad,
        String unidad,
        LocalDate fecha,
        String observaciones
) {
}
