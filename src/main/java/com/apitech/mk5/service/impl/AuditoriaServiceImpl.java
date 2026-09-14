package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.request.AuditoriaRequestDTO;
import com.apitech.mk5.dto.response.AuditoriaResponseDTO;
import com.apitech.mk5.entity.auditoria.ActorTipo;
import com.apitech.mk5.entity.auditoria.Auditoria;
import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.mapper.AuditoriaMapper;
import com.apitech.mk5.repository.auditoria.AuditoriaRepository;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import com.apitech.mk5.service.AuditoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuditoriaServiceImpl implements AuditoriaService {
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaServiceImpl(AuditoriaRepository auditoriaRepository,
                                UsuarioRepository usuarioRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public AuditoriaResponseDTO crear(AuditoriaRequestDTO dto) {
        Usuario usuario = usuarioSegunActor(dto);
        Auditoria entidad = AuditoriaMapper.toEntity(dto, usuario);
        return AuditoriaMapper.toResponseDTO(auditoriaRepository.save(entidad));
    }

    @Override
    public AuditoriaResponseDTO obtenerPorId(Integer id) {
        return AuditoriaMapper.toResponseDTO(buscarAuditoria(id));
    }

    @Override
    public List<AuditoriaResponseDTO> listar() {
        return auditoriaRepository.findAllByOrderByFechaDesc().stream()
                .map(AuditoriaMapper::toResponseDTO).toList();
    }

    @Override
    public List<AuditoriaResponseDTO> listarPorUsuario(Integer idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new RecursoNoEncontradoException("No existe un usuario con id " + idUsuario);
        }
        return auditoriaRepository.findByUsuario_IdUsuarioOrderByFechaDesc(idUsuario).stream()
                .map(AuditoriaMapper::toResponseDTO).toList();
    }

    @Override
    public List<AuditoriaResponseDTO> listarPorEntidad(String entidadTipo, Long idEntidad) {
        return auditoriaRepository.findByEntidadTipoAndIdEntidadOrderByFechaDesc(entidadTipo, idEntidad)
                .stream().map(AuditoriaMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional
    public AuditoriaResponseDTO actualizar(Integer id, AuditoriaRequestDTO dto) {
        Auditoria entidad = buscarAuditoria(id);
        Usuario usuario = usuarioSegunActor(dto);
        AuditoriaMapper.aplicarCambios(entidad, dto, usuario);
        return AuditoriaMapper.toResponseDTO(entidad);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        auditoriaRepository.delete(buscarAuditoria(id));
    }

    private Usuario usuarioSegunActor(AuditoriaRequestDTO dto) {
        if (dto.actorTipo() == ActorTipo.SISTEMA) {
            if (dto.idUsuario() != null) {
                throw new ReglaDeNegocioException("Un actor SISTEMA no puede tener id_usuario");
            }
            return null;
        }
        if (dto.idUsuario() == null) {
            throw new ReglaDeNegocioException("Un actor USUARIO debe tener id_usuario");
        }
        return usuarioRepository.findById(dto.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un usuario con id " + dto.idUsuario()));
    }

    private Auditoria buscarAuditoria(Integer id) {
        return auditoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una auditoria con id " + id));
    }
}
