package com.apitech.mk5.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Datos de entrada; la empresa se deriva de la colmena indicada. */
public record ProduccionMielRequestDTO(
        @NotNull(message = "La colmena es obligatoria")
        Integer idColmena,
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        Float cantidad,
        @NotBlank(message = "La unidad es obligatoria")
        @Size(max = 10, message = "La unidad no puede superar 10 caracteres")
        String unidad,
        @NotNull(message = "La fecha es obligatoria")
        java.time.LocalDate fecha,
        String observaciones
) {
}
