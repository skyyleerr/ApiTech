package com.apitech.mk5.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO generico para peticiones de cambio de estado (usado por
 * {@code Empresa}, {@code Usuario}, y cualquier otra entidad con un
 * metodo {@code cambiarEstado(id, nuevoEstado)} en su service). Evita
 * crear un DTO identico repetido por cada entidad.
 */
public record CambioEstadoRequestDTO(
        @NotBlank(message = "El nuevo estado es obligatorio")
        @Size(max = 50, message = "El estado no puede superar 50 caracteres")
        String nuevoEstado
) {
}
