package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.request.ProduccionMielRequestDTO;
import com.apitech.mk5.dto.response.ProduccionMielResponseDTO;
import com.apitech.mk5.entity.apiario.Colmena;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.evento.ProduccionMiel;

public final class ProduccionMielMapper {
    private ProduccionMielMapper() {
    }

    public static ProduccionMiel toEntity(ProduccionMielRequestDTO dto, Colmena colmena,
                                          Empresa empresa) {
        return new ProduccionMiel(colmena, empresa, dto.cantidad(), dto.unidad(),
                dto.fecha(), dto.observaciones());
    }

    public static void aplicarCambios(ProduccionMiel entidad, ProduccionMielRequestDTO dto,
                                      Colmena colmena, Empresa empresa) {
        entidad.setColmena(colmena);
        entidad.setEmpresa(empresa);
        entidad.setCantidad(dto.cantidad());
        entidad.setUnidad(dto.unidad());
        entidad.setFecha(dto.fecha());
        entidad.setObservaciones(dto.observaciones());
    }

    public static ProduccionMielResponseDTO toResponseDTO(ProduccionMiel entidad) {
        return new ProduccionMielResponseDTO(
                entidad.getIdProduccion(),
                entidad.getColmena() != null ? entidad.getColmena().getIdColmena() : null,
                entidad.getEmpresa() != null ? entidad.getEmpresa().getIdEmpresa() : null,
                entidad.getCantidad(), entidad.getUnidad(), entidad.getFecha(),
                entidad.getObservaciones()
        );
    }
}
