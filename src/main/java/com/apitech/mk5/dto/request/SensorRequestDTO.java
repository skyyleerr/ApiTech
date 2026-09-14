package com.apitech.mk5.dto.request;

import com.apitech.mk5.entity.apiario.TipoMedicion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de entrada para crear o actualizar un Sensor. */
public record SensorRequestDTO(

        @NotNull(message = "La empresa es obligatoria")
        Integer idEmpresa,

        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 50, message = "El codigo no puede superar 50 caracteres")
        String codigo,

        @NotNull(message = "El tipo de medicion es obligatorio")
        TipoMedicion tipo,

        @Size(max = 100)
        String modelo,

        @Size(max = 100)
        String fabricante,

        @NotNull(message = "Debe indicarse si el sensor es simulado")
        Boolean esSimulado,

        @NotBlank(message = "El estado es obligatorio")
        @Size(max = 20)
        String estado
) {
}
