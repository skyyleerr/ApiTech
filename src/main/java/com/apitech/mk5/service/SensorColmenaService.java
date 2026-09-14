package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.SensorColmenaRequestDTO;
import com.apitech.mk5.dto.response.SensorColmenaResponseDTO;

import java.util.List;

public interface SensorColmenaService {

    /**
     * Crea una asociacion sensor-colmena nueva.
     *
     * @throws com.apitech.mk5.exception.ReglaDeNegocioException si el
     *         sensor y la colmena pertenecen a empresas distintas
     *         (equivalente Java de las FK compuestas
     *         {@code fk_sc_sensor_empresa}/{@code fk_sc_colmena_empresa}),
     *         o si el sensor ya tiene otra asociacion activa.
     */
    SensorColmenaResponseDTO crear(SensorColmenaRequestDTO dto);

    SensorColmenaResponseDTO obtenerPorId(Integer id);

    List<SensorColmenaResponseDTO> listarPorColmena(Integer idColmena);

    List<SensorColmenaResponseDTO> listarPorSensor(Integer idSensor);

    /**
     * Desactiva una asociacion (fin de la instalacion del sensor en esa colmena).
     *
     * @throws com.apitech.mk5.exception.ReglaDeNegocioException si la
     *         asociacion tiene un monitoreo activo (equivalente Java de
     *         {@code trg_sensor_colmena_estado_bu}).
     */
    SensorColmenaResponseDTO desactivar(Integer id);

}
