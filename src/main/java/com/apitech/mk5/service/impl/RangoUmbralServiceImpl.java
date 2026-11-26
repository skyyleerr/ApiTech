package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.request.RangoUmbralRequestDTO;
import com.apitech.mk5.dto.response.RangoUmbralResponseDTO;
import com.apitech.mk5.entity.apiario.RangoUmbral;
import com.apitech.mk5.entity.apiario.TipoMedicion;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.exception.ValidacionException;
import com.apitech.mk5.mapper.RangoUmbralMapper;
import com.apitech.mk5.repository.apiario.RangoUmbralRepository;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.service.RangoUmbralService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RangoUmbralServiceImpl implements RangoUmbralService {

    private final RangoUmbralRepository rangoUmbralRepository;
    private final EmpresaRepository empresaRepository;

    public RangoUmbralServiceImpl(RangoUmbralRepository rangoUmbralRepository,
                                   EmpresaRepository empresaRepository) {
        this.rangoUmbralRepository = rangoUmbralRepository;
        this.empresaRepository = empresaRepository;
    }

    @Override
    @Transactional
    public RangoUmbralResponseDTO crear(RangoUmbralRequestDTO dto) {
        validarRango(dto.valorMin(), dto.valorMax());

        Empresa empresa = dto.idEmpresa() != null ? buscarEmpresaOFallar(dto.idEmpresa()) : null;
        validarNoDuplicado(dto.idEmpresa(), dto.tipoMedicion());

        RangoUmbral umbral = RangoUmbralMapper.toEntity(dto, empresa);
        rangoUmbralRepository.save(umbral);
        return RangoUmbralMapper.toResponseDTO(umbral);
    }

    @Override
    public RangoUmbralResponseDTO obtenerPorId(Integer id) {
        return RangoUmbralMapper.toResponseDTO(buscarOFallar(id));
    }

    @Override
    public List<RangoUmbralResponseDTO> listarPorEmpresa(Integer idEmpresa) {
        return rangoUmbralRepository.findByEmpresa_IdEmpresa(idEmpresa).stream()
                .map(RangoUmbralMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public RangoUmbralResponseDTO actualizar(Integer id, RangoUmbralRequestDTO dto) {
        RangoUmbral umbral = buscarOFallar(id);
        validarRango(dto.valorMin(), dto.valorMax());

        boolean combinacionCambio = !dto.tipoMedicion().equals(umbral.getTipoMedicion())
                || !java.util.Objects.equals(dto.idEmpresa(),
                        umbral.getEmpresa() != null ? umbral.getEmpresa().getIdEmpresa() : null);
        if (combinacionCambio) {
            validarNoDuplicado(dto.idEmpresa(), dto.tipoMedicion());
        }

        Empresa empresa = dto.idEmpresa() != null ? buscarEmpresaOFallar(dto.idEmpresa()) : null;
        RangoUmbralMapper.aplicarCambios(umbral, dto, empresa);
        return RangoUmbralMapper.toResponseDTO(umbral);
    }

    @Override
    public RangoUmbralResponseDTO resolverUmbralAplicable(Integer idEmpresa, TipoMedicion tipoMedicion) {
        Optional<RangoUmbral> especifico =
                rangoUmbralRepository.findByEmpresa_IdEmpresaAndTipoMedicion(idEmpresa, tipoMedicion);
        if (especifico.isPresent()) {
            return RangoUmbralMapper.toResponseDTO(especifico.get());
        }

        return rangoUmbralRepository.findByEmpresaIsNullAndTipoMedicion(tipoMedicion)
                .map(RangoUmbralMapper::toResponseDTO)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay un umbral configurado (ni especifico ni global) para el tipo " + tipoMedicion));
    }

    private void validarRango(Float valorMin, Float valorMax) {
        if (valorMin >= valorMax) {
            throw new ValidacionException("El valor minimo debe ser menor que el valor maximo");
        }
    }

    private void validarNoDuplicado(Integer idEmpresa, TipoMedicion tipoMedicion) {
        boolean existe = idEmpresa != null
                ? rangoUmbralRepository.findByEmpresa_IdEmpresaAndTipoMedicion(idEmpresa, tipoMedicion).isPresent()
                : rangoUmbralRepository.findByEmpresaIsNullAndTipoMedicion(tipoMedicion).isPresent();
        if (existe) {
            throw new ReglaDeNegocioException(
                    "Ya existe un umbral configurado para esa combinacion de empresa y tipo de medicion");
        }
    }

    private RangoUmbral buscarOFallar(Integer id) {
        return rangoUmbralRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un umbral con id " + id));
    }

    private Empresa buscarEmpresaOFallar(Integer id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una empresa con id " + id));
    }
}
