package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.request.SensorColmenaRequestDTO;
import com.apitech.mk5.dto.response.SensorColmenaResponseDTO;
import com.apitech.mk5.entity.apiario.Colmena;
import com.apitech.mk5.entity.apiario.Sensor;
import com.apitech.mk5.entity.apiario.SensorColmena;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.mapper.SensorColmenaMapper;
import com.apitech.mk5.repository.apiario.ColmenaRepository;
import com.apitech.mk5.repository.apiario.MonitoreoRepository;
import com.apitech.mk5.repository.apiario.SensorColmenaRepository;
import com.apitech.mk5.repository.apiario.SensorRepository;
import com.apitech.mk5.service.SensorColmenaService;
import com.apitech.mk5.security.UsuarioContexto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementacion de {@link SensorColmenaService}.
 *
 * <p>{@link #crear} replica dos reglas del esquema SQL a la vez: la
 * coherencia de empresa entre sensor y colmena (hoy garantizada por las
 * FK compuestas {@code fk_sc_sensor_empresa}/{@code fk_sc_colmena_empresa}),
 * y la exclusividad de asociacion activa por sensor (hoy garantizada
 * por el indice unico sobre {@code activo_uniq}).</p>
 */
@Service
@Transactional(readOnly = true)
public class SensorColmenaServiceImpl implements SensorColmenaService {

    private static final String MONITOREO_ACTIVO = "activo";

    private final SensorColmenaRepository sensorColmenaRepository;
    private final SensorRepository sensorRepository;
    private final ColmenaRepository colmenaRepository;
    private final MonitoreoRepository monitoreoRepository;
    private final UsuarioContexto usuarioContexto;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;

    public SensorColmenaServiceImpl(SensorColmenaRepository sensorColmenaRepository,
                                     SensorRepository sensorRepository,
                                     ColmenaRepository colmenaRepository,
                                     MonitoreoRepository monitoreoRepository,
                                     UsuarioContexto usuarioContexto,
                                   com.apitech.mk5.service.EmpresaOperacionService empresaOperacion) {
        this.sensorColmenaRepository = sensorColmenaRepository;
        this.sensorRepository = sensorRepository;
        this.colmenaRepository = colmenaRepository;
        this.monitoreoRepository = monitoreoRepository;
        this.usuarioContexto = usuarioContexto;
        this.empresaOperacion = empresaOperacion;
    }

    @Override
    @Transactional
    public SensorColmenaResponseDTO crear(SensorColmenaRequestDTO dto) {
        Sensor sensor = sensorRepository.findById(dto.idSensor())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un sensor con id " + dto.idSensor()));
        Colmena colmena = colmenaRepository.findById(dto.idColmena())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una colmena con id " + dto.idColmena()));

        Integer idEmpresaSensor = sensor.getEmpresa().getIdEmpresa();
        Integer idEmpresaColmena = colmena.getEmpresa().getIdEmpresa();
        Integer empresaActual = usuarioContexto.getIdEmpresaActual();
        empresaOperacion.validarPuedeOperar(idEmpresaSensor);
        if (empresaActual != null
                && (!empresaActual.equals(idEmpresaSensor) || !empresaActual.equals(idEmpresaColmena))) {
            throw new ReglaDeNegocioException("No puedes asociar sensores o colmenas de otra empresa");
        }
        if (!idEmpresaSensor.equals(idEmpresaColmena)) {
            throw new ReglaDeNegocioException(
                    "El sensor y la colmena deben pertenecer a la misma empresa");
        }

        if (sensorColmenaRepository.existsBySensor_IdSensorAndActivoTrue(sensor.getIdSensor())) {
            throw new ReglaDeNegocioException(
                    "El sensor " + sensor.getCodigo() + " ya tiene una asociacion activa; desactivela primero");
        }

        SensorColmena asociacion = new SensorColmena(sensor, colmena, idEmpresaSensor);
        sensorColmenaRepository.save(asociacion);
        return SensorColmenaMapper.toResponseDTO(asociacion);
    }

    @Override
    public SensorColmenaResponseDTO obtenerPorId(Integer id) {
        return SensorColmenaMapper.toResponseDTO(buscarOFallar(id));
    }

    @Override
    public List<SensorColmenaResponseDTO> listarPorColmena(Integer idColmena) {
        return sensorColmenaRepository.findByColmena_IdColmena(idColmena).stream()
                .map(SensorColmenaMapper::toResponseDTO)
                .toList();
    }

    @Override
    public List<SensorColmenaResponseDTO> listarPorSensor(Integer idSensor) {
        return sensorColmenaRepository.findBySensor_IdSensor(idSensor).stream()
                .map(SensorColmenaMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public SensorColmenaResponseDTO desactivar(Integer id) {
        SensorColmena asociacion = buscarOFallar(id);
        empresaOperacion.validarPuedeOperar(asociacion.getIdEmpresa());
        Integer empresaActual = usuarioContexto.getIdEmpresaActual();
        if (empresaActual != null && !empresaActual.equals(asociacion.getIdEmpresa())) {
            throw new ReglaDeNegocioException("La asociacion no pertenece a la empresa del usuario autenticado");
        }

        if (monitoreoRepository.existsByAsociacion_IdAsociacionAndEstado(id, MONITOREO_ACTIVO)) {
            throw new ReglaDeNegocioException(
                    "No se puede desactivar la asociacion porque tiene un monitoreo activo; desactivelo primero");
        }

        asociacion.setActivo(false);
        asociacion.setFechaFin(LocalDateTime.now());
        return SensorColmenaMapper.toResponseDTO(asociacion);
    }

    private SensorColmena buscarOFallar(Integer id) {
        return sensorColmenaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una asociacion con id " + id));
    }
}
