package com.apitech.mk5.dto.response;

/**
 * Referencia LIGERA a un {@code Usuario}, usada dentro de otros DTOs
 * (por ejemplo, {@code ColmenaResponseDTO.usuarioRegistro} o
 * {@code AlertaResponseDTO.usuarioResolucion}) cuando solo hace falta
 * identificarlo, no traer su empresa/rol/fechas completos como haria
 * {@link UsuarioResponseDTO}.
 */
public record UsuarioResumenDTO(
        Integer idUsuario,
        String nombre,
        String apellido,
        String correo
) {
}
