package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.response.RolResponseDTO;
import com.apitech.mk5.entity.usuario.Rol;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.mapper.RolMapper;
import com.apitech.mk5.repository.usuario.RolRepository;
import com.apitech.mk5.service.RolService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;

    public RolServiceImpl(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    public List<RolResponseDTO> listarTodos() {
        return rolRepository.findAll().stream()
                .map(RolMapper::toResponseDTO)
                .toList();
    }

    @Override
    public RolResponseDTO obtenerPorId(Integer id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un rol con id " + id));
        return RolMapper.toResponseDTO(rol);
    }
}
