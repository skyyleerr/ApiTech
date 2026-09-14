package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.ColmenaRequestDTO;
import com.apitech.mk5.dto.response.ColmenaResponseDTO;

import java.util.List;

public interface ColmenaService {

    ColmenaResponseDTO crear(ColmenaRequestDTO dto);

    ColmenaResponseDTO obtenerPorId(Integer id);

    /** Alcance correcto: siempre por empresa, nunca por usuario individual. */
    List<ColmenaResponseDTO> listarPorEmpresa(Integer idEmpresa);

    ColmenaResponseDTO actualizar(Integer id, ColmenaRequestDTO dto);

    ColmenaResponseDTO cambiarEstado(Integer id, String nuevoEstado);

}
