package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.request.ColmenaRequestDTO;
import com.apitech.mk5.dto.response.ColmenaResponseDTO;
import com.apitech.mk5.entity.apiario.Colmena;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.mapper.ColmenaMapper;
import com.apitech.mk5.repository.apiario.ColmenaRepository;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import com.apitech.mk5.service.ColmenaService;
import com.apitech.mk5.security.UsuarioContexto;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ColmenaServiceImpl implements ColmenaService {

    private final ColmenaRepository colmenaRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioContexto usuarioContexto;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;

    public ColmenaServiceImpl(ColmenaRepository colmenaRepository,
                               EmpresaRepository empresaRepository,
                               UsuarioRepository usuarioRepository,
                               UsuarioContexto usuarioContexto,
                              com.apitech.mk5.service.EmpresaOperacionService empresaOperacion) {
        this.colmenaRepository = colmenaRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioContexto = usuarioContexto;
        this.empresaOperacion = empresaOperacion;
    }

    @Override
    @Transactional
    public ColmenaResponseDTO crear(ColmenaRequestDTO dto) {
        Integer empresaActual = usuarioContexto.getIdEmpresaActual();
        Integer idEmpresa = empresaActual != null ? empresaActual : dto.idEmpresa();
        validarEmpresaSolicitada(idEmpresa, dto.idEmpresa());
        Empresa empresa = buscarEmpresaOFallar(idEmpresa);
        empresaOperacion.validarLimiteColmenas(idEmpresa, colmenaRepository.findByEmpresa_IdEmpresa(idEmpresa).size());
        Usuario usuarioRegistro = usuarioContexto.getIdEmpresaActual() != null
                ? usuarioContexto.getUsuarioActual()
                : (dto.idUsuarioRegistro() != null ? buscarUsuarioOFallar(dto.idUsuarioRegistro()) : null);

        Colmena colmena = ColmenaMapper.toEntity(dto, empresa, usuarioRegistro);
        colmenaRepository.save(colmena);
        return ColmenaMapper.toResponseDTO(colmena);
    }

    @Override
    public ColmenaResponseDTO obtenerPorId(Integer id) {
        return ColmenaMapper.toResponseDTO(buscarColmenaOFallar(id));
    }

    @Override
    public List<ColmenaResponseDTO> listarPorEmpresa(Integer idEmpresa) {
        Integer empresa = usuarioContexto.getIdEmpresaActual();
        if (empresa == null) empresa = idEmpresa;
        if (usuarioContexto.getIdEmpresaActual() != null && !usuarioContexto.getIdEmpresaActual().equals(idEmpresa)) {
            // El parametro se ignora para usuarios cliente: nunca se permite
            // enumerar la informacion de otra empresa.
            empresa = usuarioContexto.getIdEmpresaActual();
        }
        return colmenaRepository.findByEmpresa_IdEmpresa(empresa).stream()
                .map(ColmenaMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public ColmenaResponseDTO actualizar(Integer id, ColmenaRequestDTO dto) {
        Colmena colmena = buscarColmenaDeEmpresa(id);
        empresaOperacion.validarPuedeOperar(colmena.getEmpresa().getIdEmpresa());
        Empresa empresa = buscarEmpresaOFallar(dto.idEmpresa());
        Usuario usuarioRegistro = dto.idUsuarioRegistro() != null
                ? buscarUsuarioOFallar(dto.idUsuarioRegistro()) : null;

        ColmenaMapper.aplicarCambios(colmena, dto, empresa, usuarioRegistro);
        return ColmenaMapper.toResponseDTO(colmena);
    }

    @Override
    @Transactional
    public ColmenaResponseDTO cambiarEstado(Integer id, String nuevoEstado) {
        Colmena colmena = buscarColmenaDeEmpresa(id);
        empresaOperacion.validarPuedeOperar(colmena.getEmpresa().getIdEmpresa());
        colmena.setEstado(nuevoEstado);
        return ColmenaMapper.toResponseDTO(colmena);
    }

    private Colmena buscarColmenaDeEmpresa(Integer id) {
        Colmena colmena = buscarColmenaOFallar(id);
        Integer empresaActual = usuarioContexto.getIdEmpresaActual();
        if (empresaActual != null && !empresaActual.equals(colmena.getEmpresa().getIdEmpresa())) {
            throw new ReglaDeNegocioException("La colmena no pertenece a la empresa del usuario autenticado");
        }
        return colmena;
    }

    private void validarEmpresaSolicitada(Integer empresaActual, Integer empresaSolicitada) {
        if (empresaActual != null && !empresaActual.equals(empresaSolicitada)) {
            throw new ReglaDeNegocioException("No puedes operar con datos de otra empresa");
        }
    }

    private Colmena buscarColmenaOFallar(Integer id) {
        return colmenaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una colmena con id " + id));
    }

    private Empresa buscarEmpresaOFallar(Integer id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una empresa con id " + id));
    }

    private Usuario buscarUsuarioOFallar(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con id " + id));
    }
}
