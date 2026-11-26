package com.apitech.mk5.dto.request;

import com.apitech.mk5.entity.apiario.OrigenMedicion;
import com.apitech.mk5.entity.apiario.TipoMedicion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MedicionRequestDTO(

        @NotNull(message = "La asociacion sensor-colmena es obligatoria")
        Integer idAsociacion,

        @NotNull(message = "El tipo de medicion es obligatorio")
        TipoMedicion tipoMedicion,

        @NotNull(message = "El valor es obligatorio")
        Float valor,

        @NotBlank(message = "La unidad es obligatoria")
        @Size(max = 10, message = "La unidad no puede superar 10 caracteres")
        String unidad,

        @NotNull(message = "El origen es obligatorio")
        OrigenMedicion origen
) {
}
