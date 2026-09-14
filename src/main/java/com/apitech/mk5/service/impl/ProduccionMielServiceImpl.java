package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.request.ProduccionMielRequestDTO;
import com.apitech.mk5.dto.response.ProduccionMielResponseDTO;
import com.apitech.mk5.entity.apiario.Colmena;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.evento.ProduccionMiel;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.mapper.ProduccionMielMapper;
import com.apitech.mk5.repository.apiario.ColmenaRepository;
import com.apitech.mk5.repository.evento.ProduccionMielRepository;
import com.apitech.mk5.service.ProduccionMielService;
import com.apitech.mk5.security.UsuarioContexto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProduccionMielServiceImpl implements ProduccionMielService {
    private final ProduccionMielRepository produccionRepository;
    private final ColmenaRepository colmenaRepository;
    private final UsuarioContexto usuarioContexto;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;

    public ProduccionMielServiceImpl(ProduccionMielRepository produccionRepository,
                                     ColmenaRepository colmenaRepository,
                                     UsuarioContexto usuarioContexto,
                                     com.apitech.mk5.service.EmpresaOperacionService empresaOperacion) {
        this.produccionRepository = produccionRepository;
        this.colmenaRepository = colmenaRepository;
        this.usuarioContexto = usuarioContexto;
        this.empresaOperacion = empresaOperacion;
    }

    @Override
    @Transactional
    public ProduccionMielResponseDTO crear(ProduccionMielRequestDTO dto) {
        Colmena colmena = buscarColmena(dto.idColmena());
        Empresa empresa = empresaDe(colmena);
        validarEmpresaActual(empresa);
        empresaOperacion.validarPuedeOperar(empresa.getIdEmpresa());
        ProduccionMiel entidad = ProduccionMielMapper.toEntity(dto, colmena, empresa);
        return ProduccionMielMapper.toResponseDTO(produccionRepository.save(entidad));
    }

    @Override
    public ProduccionMielResponseDTO obtenerPorId(Integer id) {
        ProduccionMiel entidad = buscarProduccion(id);
        validarEmpresaActual(entidad.getEmpresa());
        return ProduccionMielMapper.toResponseDTO(entidad);
    }

    @Override
    public List<ProduccionMielResponseDTO> listarPorEmpresa(Integer idEmpresa) {
        Integer empresa = empresaSolicitada(idEmpresa);
        return produccionRepository.findByEmpresa_IdEmpresaOrderByFechaDesc(empresa)
                .stream().map(ProduccionMielMapper::toResponseDTO).toList();
    }

    @Override
    public List<ProduccionMielResponseDTO> listarPorColmena(Integer idColmena) {
        Colmena colmena = buscarColmena(idColmena);
        validarEmpresaActual(empresaDe(colmena));
        return produccionRepository.findByColmena_IdColmenaOrderByFechaDesc(idColmena)
                .stream().map(ProduccionMielMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional
    public ProduccionMielResponseDTO actualizar(Integer id, ProduccionMielRequestDTO dto) {
        ProduccionMiel entidad = buscarProduccion(id);
        validarEmpresaActual(entidad.getEmpresa());
        empresaOperacion.validarPuedeOperar(entidad.getEmpresa().getIdEmpresa());
        Colmena colmena = buscarColmena(dto.idColmena());
        Empresa empresa = empresaDe(colmena);
        ProduccionMielMapper.aplicarCambios(entidad, dto, colmena, empresa);
        return ProduccionMielMapper.toResponseDTO(entidad);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        ProduccionMiel entidad = buscarProduccion(id);
        validarEmpresaActual(entidad.getEmpresa());
        empresaOperacion.validarPuedeOperar(entidad.getEmpresa().getIdEmpresa());
        produccionRepository.delete(entidad);
    }

    private Integer empresaSolicitada(Integer idEmpresa) {
        Integer actual = usuarioContexto.getIdEmpresaActual();
        if (actual != null && !actual.equals(idEmpresa)) {
            throw new ReglaDeNegocioException("No puedes consultar la produccion de otra empresa");
        }
        return actual != null ? actual : idEmpresa;
    }

    private void validarEmpresaActual(Empresa empresa) {
        Integer actual = usuarioContexto.getIdEmpresaActual();
        if (actual != null && (empresa == null || !actual.equals(empresa.getIdEmpresa()))) {
            throw new ReglaDeNegocioException("La produccion no pertenece a la empresa del usuario autenticado");
        }
    }

    private Empresa empresaDe(Colmena colmena) {
        if (colmena.getEmpresa() == null || colmena.getEmpresa().getIdEmpresa() == null) {
            throw new ReglaDeNegocioException("La colmena debe tener una empresa para registrar produccion");
        }
        return colmena.getEmpresa();
    }

    private Colmena buscarColmena(Integer id) {
        return colmenaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una colmena con id " + id));
    }

    private ProduccionMiel buscarProduccion(Integer id) {
        return produccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una produccion con id " + id));
    }
}
