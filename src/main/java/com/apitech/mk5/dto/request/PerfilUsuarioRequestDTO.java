package com.apitech.mk5.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos editables por el propio usuario desde Configuración. */
public record PerfilUsuarioRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String nombre,

        @Size(max = 100)
        String apellido,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150)
        String correo,

        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        String passwordNuevo
) {}
