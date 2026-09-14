package com.apitech.mk5.service;

import com.apitech.mk5.dto.response.MonitoreoResponseDTO;

import java.util.List;

public interface MonitoreoService {

    /**
     * Activa el monitoreo de una asociacion sensor-colmena.
     *
     * @throws com.apitech.mk5.exception.ReglaDeNegocioException si la
     *         asociacion, el sensor o la colmena no estan en condiciones
     *         de ser monitoreados (equivalente Java de
     *         {@code trg_monitoreo_ins/upd} y {@code trg_monitoreo_empresa_bi/bu}),
     *         o si ya existe un monitoreo activo para esa asociacion.
     */
    MonitoreoResponseDTO activar(Integer idAsociacion);

    /** Detiene el monitoreo activo de una asociacion (no lo borra, lo marca como finalizado). */
    MonitoreoResponseDTO desactivar(Integer idMonitoreo);

    MonitoreoResponseDTO obtenerPorId(Integer id);

    List<MonitoreoResponseDTO> listarPorAsociacion(Integer idAsociacion);

}
