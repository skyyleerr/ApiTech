package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.NotificacionRequestDTO;
import com.apitech.mk5.dto.response.NotificacionResponseDTO;

import java.util.List;

public interface NotificacionService {
    NotificacionResponseDTO crear(NotificacionRequestDTO dto);
    NotificacionResponseDTO obtenerPorId(Integer id);
    List<NotificacionResponseDTO> listar(Integer idEmpresa);
    List<NotificacionResponseDTO> listarPorUsuario(Integer idUsuario);
    NotificacionResponseDTO actualizar(Integer id, NotificacionRequestDTO dto);
    void eliminar(Integer id);
}
