package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.request.AlertaRequestDTO;
import com.apitech.mk5.dto.response.AlertaResponseDTO;
import com.apitech.mk5.entity.apiario.Alerta;
import com.apitech.mk5.entity.apiario.Colmena;
import com.apitech.mk5.entity.apiario.Medicion;
import com.apitech.mk5.entity.apiario.Sensor;

public final class AlertaMapper {

    private AlertaMapper() {
    }

    public static Alerta toEntity(AlertaRequestDTO dto, Colmena colmena, Integer idEmpresa,
                                   Medicion medicion, Sensor sensor) {
        return new Alerta(colmena, idEmpresa, medicion, sensor, dto.tipo(), dto.descripcion(),
                dto.valorDetectado(), dto.umbralMin(), dto.umbralMax(), dto.severidad(), "ACTIVA");
    }

    public static AlertaResponseDTO toResponseDTO(Alerta alerta) {
        return new AlertaResponseDTO(
                alerta.getIdAlerta(),
                alerta.getColmena().getIdColmena(),
                alerta.getIdEmpresa(),
                alerta.getMedicion() != null ? alerta.getMedicion().getIdMedicion() : null,
                alerta.getSensor() != null ? alerta.getSensor().getIdSensor() : null,
                alerta.getTipo(),
                alerta.getDescripcion(),
                alerta.getValorDetectado(),
                alerta.getUmbralMin(),
                alerta.getUmbralMax(),
                alerta.getSeveridad(),
                alerta.getEstado(),
                alerta.getFecha(),
                alerta.getFechaResolucion(),
                alerta.getUsuarioResolucion() != null ? UsuarioResumenMapper.toResumenDTO(alerta.getUsuarioResolucion()) : null
        );
    }
}
