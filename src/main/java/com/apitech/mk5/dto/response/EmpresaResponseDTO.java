package com.apitech.mk5.dto.response;

import java.time.LocalDateTime;

/**
 * Datos de salida al exponer una {@code Empresa}. Deliberadamente NO es
 * la entidad JPA expuesta directamente -- ver la justificacion general
 * de DTOs en la documentacion de arquitectura del proyecto.
 */
public record EmpresaResponseDTO(
        Integer idEmpresa,
        String nit,
        String nombreEmpresa,
        String correo,
        String telefono,
        String direccion,
        String estado,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaActualizacion
) {
}
