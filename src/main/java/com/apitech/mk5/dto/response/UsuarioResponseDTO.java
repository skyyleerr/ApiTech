package com.apitech.mk5.dto.response;

import java.time.LocalDateTime;

/**
 * Datos de salida al exponer un {@code Usuario}.
 *
 * <p>NUNCA incluye {@code password} (ni siquiera el hash) -- es la
 * razon principal por la que este DTO existe en vez de exponer la
 * entidad {@code Usuario} directamente. {@code empresa} viene como
 * {@code null} cuando el usuario es de ApiTech (sin empresa asignada),
 * reflejando fielmente la regla de negocio del dominio.</p>
 */
public record UsuarioResponseDTO(
        Integer idUsuario,
        EmpresaResponseDTO empresa,
        RolResponseDTO rol,
        String nombre,
        String apellido,
        String correo,
        String estado,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaActualizacion,
        LocalDateTime ultimoLogin
) {
}
