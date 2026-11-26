package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.response.MonitoreoResponseDTO;
import com.apitech.mk5.entity.apiario.Monitoreo;

public final class MonitoreoMapper {

    private MonitoreoMapper() {
    }

    public static MonitoreoResponseDTO toResponseDTO(Monitoreo monitoreo) {
        return new MonitoreoResponseDTO(
                monitoreo.getIdMonitoreo(),
                monitoreo.getAsociacion().getIdAsociacion(),
                monitoreo.getEstado(),
                monitoreo.getFechaInicio(),
                monitoreo.getFechaFin()
        );
    }
}
