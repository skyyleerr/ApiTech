package com.apitech.mk5.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para cambiar la contrasena de un {@code Usuario}
 * existente. Operacion separada de la actualizacion general de datos
 * (ver {@link UsuarioActualizacionRequestDTO}).
 */
public record CambioPasswordRequestDTO(

        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 8, max = 100, message = "La contrasena debe tener entre 8 y 100 caracteres")
        String passwordNuevo
) {
}
