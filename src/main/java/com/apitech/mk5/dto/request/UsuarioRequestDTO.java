package com.apitech.mk5.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para CREAR un {@code Usuario} nuevo.
 *
 * <p>{@code password} viaja en texto plano SOLO en este DTO de entrada
 * (nunca en un DTO de salida): el {@code service} lo recibe, lo pasa
 * por {@code PasswordEncoder.encode(...)} y descarta el texto plano
 * inmediatamente -- lo unico que llega a {@code Usuario.password} es el
 * hash.</p>
 *
 * <p>{@code idEmpresa} puede ser {@code null} (usuarios ApiTech) o no
 * (usuarios cliente) -- por eso NO lleva {@code @NotNull}. La regla que
 * decide CUANDO debe o no debe ser nulo segun el rol se valida en el
 * {@code service} ({@code UsuarioServiceImpl}), no aqui: Bean
 * Validation solo puede expresar reglas sobre un campo aislado, no
 * "depende del valor de otro campo".</p>
 */
public record UsuarioRequestDTO(

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

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 100, message = "La contrasena debe tener entre 8 y 100 caracteres")
        String password,

        @NotBlank(message = "El estado es obligatorio")
        @Size(max = 20, message = "El estado no puede superar 20 caracteres")
        String estado
) {
}
