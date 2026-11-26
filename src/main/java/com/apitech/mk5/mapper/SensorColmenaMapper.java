package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.response.SensorColmenaResponseDTO;
import com.apitech.mk5.entity.apiario.SensorColmena;

public final class SensorColmenaMapper {

    private SensorColmenaMapper() {
    }

    public static SensorColmenaResponseDTO toResponseDTO(SensorColmena asociacion) {
        return new SensorColmenaResponseDTO(
                asociacion.getIdAsociacion(),
                SensorMapper.toResponseDTO(asociacion.getSensor()),
                ColmenaMapper.toResponseDTO(asociacion.getColmena()),
                asociacion.getFechaInicio(),
                asociacion.getFechaFin(),
                asociacion.isActivo()
        );
    }
}
