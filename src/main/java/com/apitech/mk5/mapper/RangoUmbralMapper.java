package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.request.RangoUmbralRequestDTO;
import com.apitech.mk5.dto.response.RangoUmbralResponseDTO;
import com.apitech.mk5.entity.apiario.RangoUmbral;
import com.apitech.mk5.entity.empresa.Empresa;

public final class RangoUmbralMapper {

    private RangoUmbralMapper() {
    }

    public static RangoUmbral toEntity(RangoUmbralRequestDTO dto, Empresa empresa) {
        return new RangoUmbral(empresa, dto.tipoMedicion(), dto.valorMin(), dto.valorMax(),
                dto.unidad(), dto.estado());
    }

    public static void aplicarCambios(RangoUmbral entidad, RangoUmbralRequestDTO dto, Empresa empresa) {
        entidad.setEmpresa(empresa);
        entidad.setTipoMedicion(dto.tipoMedicion());
        entidad.setValorMin(dto.valorMin());
        entidad.setValorMax(dto.valorMax());
        entidad.setUnidad(dto.unidad());
        entidad.setEstado(dto.estado());
    }

    public static RangoUmbralResponseDTO toResponseDTO(RangoUmbral umbral) {
        return new RangoUmbralResponseDTO(
                umbral.getIdUmbral(),
                umbral.getEmpresa() != null ? EmpresaMapper.toResponseDTO(umbral.getEmpresa()) : null,
                umbral.getTipoMedicion(),
                umbral.getValorMin(),
                umbral.getValorMax(),
                umbral.getUnidad(),
                umbral.getEstado()
        );
    }
}
