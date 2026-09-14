package com.apitech.mk5.dto.response;

import java.time.LocalDateTime;

/**
 * Datos de salida al exponer una Colmena.
 *
 * <p>{@code usuarioRegistro} usa {@link UsuarioResumenDTO} (no
 * {@link UsuarioResponseDTO}) por la misma razon documentada en
 * {@code AlertaResponseDTO.usuarioResolucion}: aqui solo hace falta
 * identificar quien la registro, no traer su empresa/rol completos.</p>
 */
public record ColmenaResponseDTO(
        Integer idColmena,
        EmpresaResponseDTO empresa,
        UsuarioResumenDTO usuarioRegistro,
        String nombre,
        String ubicacion,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
