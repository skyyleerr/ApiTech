package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.ProduccionMielRequestDTO;
import com.apitech.mk5.dto.response.ProduccionMielResponseDTO;

import java.util.List;

public interface ProduccionMielService {
    ProduccionMielResponseDTO crear(ProduccionMielRequestDTO dto);
    ProduccionMielResponseDTO obtenerPorId(Integer id);
    List<ProduccionMielResponseDTO> listarPorEmpresa(Integer idEmpresa);
    List<ProduccionMielResponseDTO> listarPorColmena(Integer idColmena);
    ProduccionMielResponseDTO actualizar(Integer id, ProduccionMielRequestDTO dto);
    void eliminar(Integer id);
}
