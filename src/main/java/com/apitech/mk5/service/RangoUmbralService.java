package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.RangoUmbralRequestDTO;
import com.apitech.mk5.dto.response.RangoUmbralResponseDTO;
import com.apitech.mk5.entity.apiario.TipoMedicion;

import java.util.List;

public interface RangoUmbralService {

    /**
     * @throws com.apitech.mk5.exception.ValidacionException si
     *         {@code valorMin >= valorMax}.
     * @throws com.apitech.mk5.exception.ReglaDeNegocioException si ya
     *         existe un umbral para esa combinacion empresa+tipo (o
     *         global+tipo).
     */
    RangoUmbralResponseDTO crear(RangoUmbralRequestDTO dto);

    RangoUmbralResponseDTO obtenerPorId(Integer id);

    List<RangoUmbralResponseDTO> listarPorEmpresa(Integer idEmpresa);

    RangoUmbralResponseDTO actualizar(Integer id, RangoUmbralRequestDTO dto);

    /**
     * Resuelve el umbral aplicable a una empresa para un tipo de
     * medicion: primero busca uno especifico de esa empresa; si no
     * existe, usa el global. Esta es la regla de prioridad documentada
     * en el esquema SQL, implementada aqui por primera vez.
     *
     * @throws com.apitech.mk5.exception.RecursoNoEncontradoException si
     *         no existe ni un umbral especifico ni uno global para ese tipo.
     */
    RangoUmbralResponseDTO resolverUmbralAplicable(Integer idEmpresa, TipoMedicion tipoMedicion);

}
