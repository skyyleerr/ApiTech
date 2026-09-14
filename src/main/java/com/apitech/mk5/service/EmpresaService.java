package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.EmpresaRequestDTO;
import com.apitech.mk5.dto.response.EmpresaResponseDTO;

import java.util.List;

/**
 * Logica de negocio de {@code Empresa}. Este es el punto de integracion
 * real para quien consuma este nucleo (controllers REST propios,
 * futuros controllers MVC de Thymeleaf, o el modulo de reportes) -- se
 * consume esta interfaz, nunca {@code EmpresaRepository} directamente.
 */
public interface EmpresaService {

    /**
     * Crea una empresa nueva.
     *
     * @throws com.apitech.mk5.exception.ReglaDeNegocioException si ya
     *         existe una empresa con el mismo NIT.
     */
    EmpresaResponseDTO crear(EmpresaRequestDTO dto);

    /**
     * @throws com.apitech.mk5.exception.RecursoNoEncontradoException
     *         si no existe una empresa con ese id.
     */
    EmpresaResponseDTO obtenerPorId(Integer id);

    List<EmpresaResponseDTO> listarTodas();

    /**
     * Actualiza los datos de una empresa existente.
     *
     * @throws com.apitech.mk5.exception.RecursoNoEncontradoException
     *         si no existe una empresa con ese id.
     * @throws com.apitech.mk5.exception.ReglaDeNegocioException si el
     *         nuevo NIT ya pertenece a otra empresa.
     */
    EmpresaResponseDTO actualizar(Integer id, EmpresaRequestDTO dto);

    /**
     * Cambia el estado de una empresa (por ejemplo, "activa" -&gt;
     * "inactiva"). No existe un metodo de borrado fisico: casi todas
     * las FK del esquema son {@code ON DELETE RESTRICT}, el modelo esta
     * disenado para dar de baja logicamente, no para eliminar filas.
     *
     * @throws com.apitech.mk5.exception.RecursoNoEncontradoException
     *         si no existe una empresa con ese id.
     */
    EmpresaResponseDTO cambiarEstado(Integer id, String nuevoEstado);

}
