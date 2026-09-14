package com.apitech.mk5.dto.request;

import com.apitech.mk5.entity.apiario.TipoMedicion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code idEmpresa} es {@code null} para crear un umbral GLOBAL (no
 * lleva {@code @NotNull} a proposito). La regla
 * {@code valorMin < valorMax} no se puede expresar con anotaciones de
 * un solo campo -- se valida en {@code RangoUmbralServiceImpl}.
 */
public record RangoUmbralRequestDTO(

        Integer idEmpresa,

        @NotNull(message = "El tipo de medicion es obligatorio")
        TipoMedicion tipoMedicion,

        @NotNull(message = "El valor minimo es obligatorio")
        Float valorMin,

        @NotNull(message = "El valor maximo es obligatorio")
        Float valorMax,

        @NotBlank(message = "La unidad es obligatoria")
        @Size(max = 10, message = "La unidad no puede superar 10 caracteres")
        String unidad,

        @NotBlank(message = "El estado es obligatorio")
        @Size(max = 20, message = "El estado no puede superar 20 caracteres")
        String estado
) {
}
