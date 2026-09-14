package com.apitech.mk5.dto.response;

import com.apitech.mk5.entity.apiario.TipoMedicion;

import java.time.LocalDateTime;

/** Datos de salida al exponer un Sensor. */
public record SensorResponseDTO(
        Integer idSensor,
        EmpresaResponseDTO empresa,
        String codigo,
        TipoMedicion tipo,
        String modelo,
        String fabricante,
        boolean esSimulado,
        String estado,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaActualizacion,
        LocalDateTime fechaActivacion,
        LocalDateTime fechaDesactivacion
) {
}
