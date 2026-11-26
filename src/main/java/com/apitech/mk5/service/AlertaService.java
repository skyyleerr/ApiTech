package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.AlertaRequestDTO;
import com.apitech.mk5.dto.response.AlertaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AlertaService {

    /**
     * Crea una alerta nueva. {@code idEmpresa} se deriva automaticamente
     * de la colmena (nunca se pide al cliente de la API).
     *
     * @throws com.apitech.mk5.exception.ReglaDeNegocioException si el
     *         sensor u la medicion indicados no son coherentes con la
     *         colmena (equivalente Java de {@code trg_alertas_integridad_bi/bu}).
     */
    AlertaResponseDTO crear(AlertaRequestDTO dto);

    AlertaResponseDTO obtenerPorId(Integer id);

    Page<AlertaResponseDTO> listarPorEmpresaYEstado(Integer idEmpresa, String estado, Pageable pageable);

    Page<AlertaResponseDTO> listarPorColmena(Integer idColmena, Pageable pageable);

    /**
     * Marca una alerta como resuelta.
     *
     * @throws com.apitech.mk5.exception.RecursoNoEncontradoException si
     *         la alerta o el usuario indicados no existen.
     */
    AlertaResponseDTO resolver(Integer id, Integer idUsuarioResolucion);

}
