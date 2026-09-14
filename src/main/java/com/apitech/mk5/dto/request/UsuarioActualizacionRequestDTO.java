package com.apitech.mk5.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para ACTUALIZAR un {@code Usuario} existente.
 *
 * <p>Deliberadamente NO incluye {@code password}: cambiar la
 * contrasena es una operacion distinta (con implicaciones de
 * seguridad propias -- por ejemplo, podria requerir mas adelante
 * confirmar la contrasena actual), asi que tiene su propio DTO
 * ({@link CambioPasswordRequestDTO}) y su propio metodo de
 * {@code service}, en vez de mezclarse con "actualizar datos
 * generales" y arriesgar que una actualizacion descuidada borre o
 * sobreescriba la contrasena sin querer.</p>
 */
public record UsuarioActualizacionRequestDTO(

        Integer idEmpresa,

        @NotNull(message = "El rol es obligatorio")
        Integer idRol,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
        String apellido,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        @NotBlank(message = "El estado es obligatorio")
        @Size(max = 20, message = "El estado no puede superar 20 caracteres")
        String estado
) {
}
