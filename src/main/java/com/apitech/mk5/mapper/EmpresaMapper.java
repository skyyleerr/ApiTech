package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.request.EmpresaRequestDTO;
import com.apitech.mk5.dto.response.EmpresaResponseDTO;
import com.apitech.mk5.entity.empresa.Empresa;

/**
 * Conversion entre {@link Empresa} y sus DTOs. Clase final con metodos
 * estaticos: no tiene estado ni dependencias que inyectar, es
 * transformacion pura -- no hace falta que Spring la gestione como bean.
 */
public final class EmpresaMapper {

    private EmpresaMapper() {
        // Clase de utilidades: no debe instanciarse.
    }

    /**
     * Crea una {@link Empresa} NUEVA (sin id, lo asigna la base de
     * datos al guardar) a partir de los datos de entrada.
     */
    public static Empresa toEntity(EmpresaRequestDTO dto) {
        return new Empresa(
                dto.nit(),
                dto.nombreEmpresa(),
                dto.correo(),
                dto.telefono(),
                dto.direccion(),
                dto.estado()
        );
    }

    /**
     * Aplica los datos de un DTO de entrada sobre una entidad YA
     * EXISTENTE (para actualizar), en vez de crear una instancia nueva.
     */
    public static void aplicarCambios(Empresa entidad, EmpresaRequestDTO dto) {
        entidad.setNit(dto.nit());
        entidad.setNombreEmpresa(dto.nombreEmpresa());
        entidad.setCorreo(dto.correo());
        entidad.setTelefono(dto.telefono());
        entidad.setDireccion(dto.direccion());
        entidad.setEstado(dto.estado());
    }

    public static EmpresaResponseDTO toResponseDTO(Empresa empresa) {
        return new EmpresaResponseDTO(
                empresa.getIdEmpresa(),
                empresa.getNit(),
                empresa.getNombreEmpresa(),
                empresa.getCorreo(),
                empresa.getTelefono(),
                empresa.getDireccion(),
                empresa.getEstado(),
                empresa.getFechaRegistro(),
                empresa.getFechaActualizacion()
        );
    }
}
