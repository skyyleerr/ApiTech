package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.SensorRequestDTO;
import com.apitech.mk5.dto.response.SensorResponseDTO;

import java.util.List;

public interface SensorService {

    /** @throws com.apitech.mk5.exception.ReglaDeNegocioException si el codigo ya esta en uso. */
    SensorResponseDTO crear(SensorRequestDTO dto);

    SensorResponseDTO obtenerPorId(Integer id);

    List<SensorResponseDTO> listarPorEmpresa(Integer idEmpresa);

    SensorResponseDTO actualizar(Integer id, SensorRequestDTO dto);

    /**
     * Cambia el estado del sensor (por ejemplo, a "inactivo").
     *
     * @throws com.apitech.mk5.exception.ReglaDeNegocioException si se
     *         intenta desactivar un sensor que tiene un monitoreo
     *         activo (equivalente Java de {@code trg_sensores_estado_bu}).
     */
    SensorResponseDTO cambiarEstado(Integer id, String nuevoEstado);

}
