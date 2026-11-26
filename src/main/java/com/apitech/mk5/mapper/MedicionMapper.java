package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.response.MedicionResponseDTO;
import com.apitech.mk5.entity.apiario.Medicion;
import com.apitech.mk5.entity.apiario.SensorColmena;

/**
 * A diferencia de {@code SensorColmenaMapper}, aqui NO se anida el
 * grafo completo de {@code sensor}/{@code colmena}/{@code empresa} --
 * {@code mediciones} es la tabla de mayor volumen del sistema, y
 * cargar ese grafo completo en cada fila de un listado paginado seria
 * costoso sin necesidad. El DTO solo expone {@code idAsociacion}; quien
 * necesite los datos completos de la asociacion los pide aparte, una
 * sola vez, con {@code SensorColmenaService}.
 */
public final class MedicionMapper {

    private MedicionMapper() {
    }

    public static Medicion toEntity(SensorColmena asociacion, com.apitech.mk5.dto.request.MedicionRequestDTO dto) {
        return new Medicion(asociacion, dto.tipoMedicion(), dto.valor(), dto.unidad(), dto.origen());
    }

    public static MedicionResponseDTO toResponseDTO(Medicion medicion) {
        return new MedicionResponseDTO(
                medicion.getIdMedicion(),
                medicion.getAsociacion().getIdAsociacion(),
                medicion.getTipoMedicion(),
                medicion.getValor(),
                medicion.getUnidad(),
                medicion.getOrigen(),
                medicion.getFecha()
        );
    }
}
