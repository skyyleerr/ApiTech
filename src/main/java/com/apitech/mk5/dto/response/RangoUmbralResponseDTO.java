package com.apitech.mk5.dto.response;

import com.apitech.mk5.entity.apiario.TipoMedicion;

public record RangoUmbralResponseDTO(
        Integer idUmbral,
        EmpresaResponseDTO empresa,
        TipoMedicion tipoMedicion,
        float valorMin,
        float valorMax,
        String unidad,
        String estado
) {
}
