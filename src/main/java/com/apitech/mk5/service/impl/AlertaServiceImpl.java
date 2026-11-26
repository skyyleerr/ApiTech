package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.request.AlertaRequestDTO;
import com.apitech.mk5.dto.response.AlertaResponseDTO;
import com.apitech.mk5.entity.apiario.Alerta;
import com.apitech.mk5.entity.apiario.Colmena;
import com.apitech.mk5.entity.apiario.Medicion;
import com.apitech.mk5.entity.apiario.Sensor;
import com.apitech.mk5.entity.apiario.SensorColmena;
import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.mapper.AlertaMapper;
import com.apitech.mk5.repository.apiario.AlertaRepository;
import com.apitech.mk5.repository.apiario.ColmenaRepository;
import com.apitech.mk5.repository.apiario.MedicionRepository;
import com.apitech.mk5.repository.apiario.SensorColmenaRepository;
import com.apitech.mk5.repository.apiario.SensorRepository;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import com.apitech.mk5.service.AlertaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementacion de {@link AlertaService}.
 *
 * <p>{@link #crear} replica {@code trg_alertas_empresa_bi} (deriva
 * {@code idEmpresa} de la colmena, nunca confia en un valor externo) y
 * {@code trg_alertas_integridad_bi} (coherencia de sensor/medicion con
 * la colmena indicada).</p>
 */
@Service
@Transactional(readOnly = true)
public class AlertaServiceImpl implements AlertaService {

    private static final String ESTADO_RESUELTA = "RESUELTA";

    private final AlertaRepository alertaRepository;
    private final ColmenaRepository colmenaRepository;
    private final SensorRepository sensorRepository;
    private final MedicionRepository medicionRepository;
    private final SensorColmenaRepository sensorColmenaRepository;
    private final UsuarioRepository usuarioRepository;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;
    private final com.apitech.mk5.security.UsuarioContexto usuarioContexto;

    public AlertaServiceImpl(AlertaRepository alertaRepository,
                              ColmenaRepository colmenaRepository,
                              SensorRepository sensorRepository,
                              MedicionRepository medicionRepository,
                              SensorColmenaRepository sensorColmenaRepository,
                              UsuarioRepository usuarioRepository,
                              com.apitech.mk5.service.EmpresaOperacionService empresaOperacion,
                              com.apitech.mk5.security.UsuarioContexto usuarioContexto) {
        this.alertaRepository = alertaRepository;
        this.colmenaRepository = colmenaRepository;
        this.sensorRepository = sensorRepository;
        this.medicionRepository = medicionRepository;
        this.sensorColmenaRepository = sensorColmenaRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaOperacion = empresaOperacion;
        this.usuarioContexto = usuarioContexto;
    }

    @Override
    @Transactional
    public AlertaResponseDTO crear(AlertaRequestDTO dto) {
        Colmena colmena = colmenaRepository.findById(dto.idColmena())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una colmena con id " + dto.idColmena()));
        Integer idEmpresa = colmena.getEmpresa().getIdEmpresa();
        empresaOperacion.validarPuedeOperar(idEmpresa);

        Sensor sensor = null;
        if (dto.idSensor() != null) {
            sensor = sensorRepository.findById(dto.idSensor())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe un sensor con id " + dto.idSensor()));
            validarSensorPerteneceAColmena(sensor.getIdSensor(), colmena.getIdColmena());
        }

        Medicion medicion = null;
        if (dto.idMedicion() != null) {
            medicion = medicionRepository.findById(dto.idMedicion())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe una medicion con id " + dto.idMedicion()));
            validarMedicionCoherente(medicion, colmena.getIdColmena(), dto.idSensor());
        }

        Alerta alerta = AlertaMapper.toEntity(dto, colmena, idEmpresa, medicion, sensor);
        alertaRepository.save(alerta);
        return AlertaMapper.toResponseDTO(alerta);
    }

    @Override
    public AlertaResponseDTO obtenerPorId(Integer id) {
        return AlertaMapper.toResponseDTO(buscarOFallar(id));
    }

    @Override
    public Page<AlertaResponseDTO> listarPorEmpresaYEstado(Integer idEmpresa, String estado, Pageable pageable) {
        return alertaRepository.findByIdEmpresaAndEstado(idEmpresa, estado, pageable)
                .map(AlertaMapper::toResponseDTO);
    }

    @Override
    public Page<AlertaResponseDTO> listarPorColmena(Integer idColmena, Pageable pageable) {
        return alertaRepository.findByColmena_IdColmena(idColmena, pageable)
                .map(AlertaMapper::toResponseDTO);
    }

    @Override
    @Transactional
    public AlertaResponseDTO resolver(Integer id, Integer idUsuarioResolucion) {
        Alerta alerta = buscarOFallar(id);
        if (alerta.getIdEmpresa() != null) {
            Integer actual = usuarioContexto.getIdEmpresaActual();
            if (actual != null && !actual.equals(alerta.getIdEmpresa())) throw new ReglaDeNegocioException("La alerta no pertenece a tu empresa");
        }
        Usuario usuario = usuarioRepository.findById(idUsuarioResolucion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con id " + idUsuarioResolucion));

        alerta.setEstado(ESTADO_RESUELTA);
        alerta.setFechaResolucion(LocalDateTime.now());
        alerta.setUsuarioResolucion(usuario);
        return AlertaMapper.toResponseDTO(alerta);
    }

    /** Replica parte de {@code trg_alertas_integridad_bi}: el sensor debe tener una asociacion ACTIVA con la colmena. */
    private void validarSensorPerteneceAColmena(Integer idSensor, Integer idColmena) {
        SensorColmena activa = sensorColmenaRepository.findBySensor_IdSensorAndActivoTrue(idSensor).orElse(null);
        if (activa == null || !activa.getColmena().getIdColmena().equals(idColmena)) {
            throw new ReglaDeNegocioException(
                    "El sensor indicado no tiene una asociacion activa con la colmena indicada");
        }
    }

    /** Replica parte de {@code trg_alertas_integridad_bi}: la medicion debe corresponder a la misma colmena (y sensor, si se indico). */
    private void validarMedicionCoherente(Medicion medicion, Integer idColmena, Integer idSensor) {
        SensorColmena asociacion = medicion.getAsociacion();
        boolean colmenaCoincide = asociacion.getColmena().getIdColmena().equals(idColmena);
        boolean sensorCoincide = idSensor == null || asociacion.getSensor().getIdSensor().equals(idSensor);
        if (!colmenaCoincide || !sensorCoincide) {
            throw new ReglaDeNegocioException(
                    "La medicion indicada no es coherente con la colmena o el sensor indicados");
        }
    }

    private Alerta buscarOFallar(Integer id) {
        return alertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una alerta con id " + id));
    }
}
