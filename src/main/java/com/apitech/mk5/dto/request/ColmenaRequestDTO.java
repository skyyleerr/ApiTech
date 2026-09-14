package com.apitech.mk5.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de entrada para crear o actualizar una Colmena. */
public record ColmenaRequestDTO(

        @NotNull(message = "La empresa es obligatoria")
        Integer idEmpresa,

        Integer idUsuarioRegistro,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 150, message = "La ubicacion no puede superar 150 caracteres")
        String ubicacion,

        @NotBlank(message = "El estado es obligatorio")
        @Size(max = 50, message = "El estado no puede superar 50 caracteres")
        String estado
) {
}
