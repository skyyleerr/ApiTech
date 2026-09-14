package com.apitech.mk5.dto.response;

/**
 * Datos de salida al exponer un {@code Rol}. No existe un
 * {@code RolRequestDTO} todavia porque los roles son un catalogo fijo
 * (Admin_ApiTech, Empleado_ApiTech, Admin_Cliente, Empleado_Cliente)
 * que no se espera crear ni editar desde la API en esta etapa del
 * proyecto -- solo se consulta para asignarlo a un {@code Usuario}.
 */
public record RolResponseDTO(
        Integer idRol,
        String nombreRol,
        String descripcion
) {
}
