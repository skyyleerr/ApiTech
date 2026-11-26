package com.apitech.mk5.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para crear o actualizar una {@code Empresa}.
 *
 * <p>Es un {@code record}, igual que {@code ErrorResponse} de la capa
 * de excepciones: un DTO no tiene ciclo de vida gestionado por
 * Hibernate, no necesita constructor vacio ni setters -- se crea
 * completo de una vez a partir del cuerpo JSON de la peticion.</p>
 *
 * <p>Las anotaciones de Bean Validation se disparan automaticamente
 * cuando el controller (etapa futura) reciba este DTO con
 * {@code @Valid} -- si fallan, {@code GlobalExceptionHandler} ya sabe
 * traducirlas a una respuesta 400 con el detalle de cada campo (ver
 * Etapa 7).</p>
 *
 * <p>No incluye {@code idEmpresa} ni las fechas: esos los asigna la
 * base de datos, nunca el cliente de la API.</p>
 */
public record EmpresaRequestDTO(

        @NotBlank(message = "El NIT es obligatorio")
        @Size(max = 20, message = "El NIT no puede superar 20 caracteres")
        String nit,

        @NotBlank(message = "El nombre de la empresa es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombreEmpresa,

        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        @Size(max = 30, message = "El telefono no puede superar 30 caracteres")
        String telefono,

        @Size(max = 200, message = "La direccion no puede superar 200 caracteres")
        String direccion,

        @NotBlank(message = "El estado es obligatorio")
        @Size(max = 20, message = "El estado no puede superar 20 caracteres")
        String estado
) {
}
