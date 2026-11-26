package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.AuditoriaRequestDTO;
import com.apitech.mk5.dto.response.AuditoriaResponseDTO;

import java.util.List;

public interface AuditoriaService {
    AuditoriaResponseDTO crear(AuditoriaRequestDTO dto);
    AuditoriaResponseDTO obtenerPorId(Integer id);
    List<AuditoriaResponseDTO> listar();
    List<AuditoriaResponseDTO> listarPorUsuario(Integer idUsuario);
    List<AuditoriaResponseDTO> listarPorEntidad(String entidadTipo, Long idEntidad);
    AuditoriaResponseDTO actualizar(Integer id, AuditoriaRequestDTO dto);
    void eliminar(Integer id);
}
