package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.request.EmpresaRequestDTO;
import com.apitech.mk5.dto.response.EmpresaResponseDTO;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.mapper.EmpresaMapper;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.service.EmpresaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementacion de {@link EmpresaService}.
 *
 * <p>{@code @Transactional(readOnly = true)} a nivel de clase: la
 * mayoria de los metodos son lecturas, asi que ese es el
 * comportamiento por defecto; los metodos que escriben
 * ({@link #crear}, {@link #actualizar}, {@link #cambiarEstado})
 * sobrescriben la anotacion sin {@code readOnly} para permitir
 * modificaciones dentro de una transaccion real.</p>
 */
@Service
@Transactional(readOnly = true)
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaServiceImpl(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @Override
    @Transactional
    public EmpresaResponseDTO crear(EmpresaRequestDTO dto) {
        if (empresaRepository.existsByNit(dto.nit())) {
            throw new ReglaDeNegocioException(
                    "Ya existe una empresa registrada con el NIT " + dto.nit());
        }
        Empresa empresa = EmpresaMapper.toEntity(dto);
        empresaRepository.save(empresa);
        return EmpresaMapper.toResponseDTO(empresa);
    }

    @Override
    public EmpresaResponseDTO obtenerPorId(Integer id) {
        Empresa empresa = buscarOFallar(id);
        return EmpresaMapper.toResponseDTO(empresa);
    }

    @Override
    public List<EmpresaResponseDTO> listarTodas() {
        return empresaRepository.findAll().stream()
                .map(EmpresaMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public EmpresaResponseDTO actualizar(Integer id, EmpresaRequestDTO dto) {
        Empresa empresa = buscarOFallar(id);

        boolean nitCambio = !empresa.getNit().equals(dto.nit());
        if (nitCambio && empresaRepository.existsByNit(dto.nit())) {
            throw new ReglaDeNegocioException(
                    "Ya existe otra empresa registrada con el NIT " + dto.nit());
        }

        EmpresaMapper.aplicarCambios(empresa, dto);
        // No hace falta un empresaRepository.save(empresa) explicito aqui:
        // dentro de una transaccion, Hibernate detecta los cambios sobre
        // una entidad ya gestionada ("dirty checking") y genera el UPDATE
        // automaticamente al finalizar el metodo.
        return EmpresaMapper.toResponseDTO(empresa);
    }

    @Override
    @Transactional
    public EmpresaResponseDTO cambiarEstado(Integer id, String nuevoEstado) {
        Empresa empresa = buscarOFallar(id);
        empresa.setEstado(nuevoEstado);
        return EmpresaMapper.toResponseDTO(empresa);
    }

    private Empresa buscarOFallar(Integer id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una empresa con id " + id));
    }
}
