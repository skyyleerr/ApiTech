package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.MedicionRequestDTO;
import com.apitech.mk5.dto.response.MedicionResponseDTO;
import com.apitech.mk5.entity.apiario.TipoMedicion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface MedicionService {

    /**
     * Registra una medicion nueva.
     *
     * @throws com.apitech.mk5.exception.ReglaDeNegocioException si la
     *         asociacion no tiene un monitoreo activo, si el tipo no
     *         coincide con el del sensor, o si el origen no coincide
     *         con {@code Sensor.esSimulado} (equivalente Java de
     *         {@code trg_mediciones_coherencia_bi/bu}).
     */
    MedicionResponseDTO registrar(MedicionRequestDTO dto);

    MedicionResponseDTO obtenerPorId(Long id);

    /** Historial paginado -- nunca una lista sin acotar (ver Etapa 5). */
    Page<MedicionResponseDTO> listarPorAsociacion(Integer idAsociacion, Pageable pageable);

    Page<MedicionResponseDTO> listarPorRango(Integer idAsociacion, TipoMedicion tipo,
                                              LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    /** @throws com.apitech.mk5.exception.RecursoNoEncontradoException si la asociacion no tiene mediciones. */
    MedicionResponseDTO obtenerUltima(Integer idAsociacion);

}
