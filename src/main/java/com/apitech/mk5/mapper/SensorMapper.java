package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.request.SensorRequestDTO;
import com.apitech.mk5.dto.response.SensorResponseDTO;
import com.apitech.mk5.entity.apiario.Sensor;
import com.apitech.mk5.entity.empresa.Empresa;

public final class SensorMapper {

    private SensorMapper() {
    }

    public static Sensor toEntity(SensorRequestDTO dto, Empresa empresa) {
        return new Sensor(empresa, dto.codigo(), dto.tipo(), dto.modelo(),
                dto.fabricante(), dto.esSimulado(), dto.estado());
    }

    public static void aplicarCambios(Sensor entidad, SensorRequestDTO dto, Empresa empresa) {
        entidad.setEmpresa(empresa);
        entidad.setCodigo(dto.codigo());
        entidad.setTipo(dto.tipo());
        entidad.setModelo(dto.modelo());
        entidad.setFabricante(dto.fabricante());
        entidad.setEsSimulado(dto.esSimulado());
        entidad.setEstado(dto.estado());
    }

    public static SensorResponseDTO toResponseDTO(Sensor sensor) {
        return new SensorResponseDTO(
                sensor.getIdSensor(),
                EmpresaMapper.toResponseDTO(sensor.getEmpresa()),
                sensor.getCodigo(),
                sensor.getTipo(),
                sensor.getModelo(),
                sensor.getFabricante(),
                sensor.isEsSimulado(),
                sensor.getEstado(),
                sensor.getFechaRegistro(),
                sensor.getFechaActualizacion(),
                sensor.getFechaActivacion(),
                sensor.getFechaDesactivacion()
        );
    }
}
