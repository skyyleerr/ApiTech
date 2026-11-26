package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.request.MedicionRequestDTO;
import com.apitech.mk5.dto.response.MedicionResponseDTO;
import com.apitech.mk5.entity.apiario.Medicion;
import com.apitech.mk5.entity.apiario.OrigenMedicion;
import com.apitech.mk5.entity.apiario.Sensor;
import com.apitech.mk5.entity.apiario.SensorColmena;
import com.apitech.mk5.entity.apiario.TipoMedicion;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.mapper.MedicionMapper;
import com.apitech.mk5.repository.apiario.MedicionRepository;
import com.apitech.mk5.repository.apiario.MonitoreoRepository;
import com.apitech.mk5.repository.apiario.SensorColmenaRepository;
import com.apitech.mk5.service.MedicionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementacion de {@link MedicionService}.
 *
 * <p>{@link #registrar} replica {@code trg_mediciones_coherencia_bi/bu}
 * -- las tres condiciones que hoy exige ese trigger antes de aceptar
 * una medicion.</p>
 *
 * <p><b>Fuera de alcance deliberado:</b> este metodo NO genera
 * automaticamente una {@code Alerta} cuando el valor cae fuera del
 * {@code RangoUmbral} aplicable. Esa integracion (Medicion -> comparar
 * contra RangoUmbral -> crear Alerta si corresponde) es una
 * funcionalidad real pendiente, no incluida en esta etapa para no
 * mezclar dos responsabilidades en un mismo metodo sin que el equipo
 * lo haya decidido explicitamente.</p>
 */
@Service
@Transactional(readOnly = true)
public class MedicionServiceImpl implements MedicionService {

    private static final String MONITOREO_ACTIVO = "activo";

    private final MedicionRepository medicionRepository;
    private final SensorColmenaRepository sensorColmenaRepository;
    private final MonitoreoRepository monitoreoRepository;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;

    public MedicionServiceImpl(MedicionRepository medicionRepository,
                                SensorColmenaRepository sensorColmenaRepository,
                                MonitoreoRepository monitoreoRepository,
                                com.apitech.mk5.service.EmpresaOperacionService empresaOperacion) {
        this.medicionRepository = medicionRepository;
        this.sensorColmenaRepository = sensorColmenaRepository;
        this.monitoreoRepository = monitoreoRepository;
        this.empresaOperacion = empresaOperacion;
    }

    @Override
    @Transactional
    public MedicionResponseDTO registrar(MedicionRequestDTO dto) {
        SensorColmena asociacion = sensorColmenaRepository.findById(dto.idAsociacion())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una asociacion sensor-colmena con id " + dto.idAsociacion()));

        if (!monitoreoRepository.existsByAsociacion_IdAsociacionAndEstado(dto.idAsociacion(), MONITOREO_ACTIVO)) {
            throw new ReglaDeNegocioException(
                    "No se puede registrar una medicion: la asociacion no tiene un monitoreo activo");
        }

        empresaOperacion.validarPuedeOperar(asociacion.getIdEmpresa());

        Sensor sensor = asociacion.getSensor();
        validarCoherenciaTipo(sensor.getTipo(), dto.tipoMedicion());
        validarCoherenciaOrigen(sensor.isEsSimulado(), dto.origen());

        Medicion medicion = MedicionMapper.toEntity(asociacion, dto);
        medicionRepository.save(medicion);
        return MedicionMapper.toResponseDTO(medicion);
    }

    @Override
    public MedicionResponseDTO obtenerPorId(Long id) {
        return MedicionMapper.toResponseDTO(
                medicionRepository.findById(id)
                        .orElseThrow(() -> new RecursoNoEncontradoException("No existe una medicion con id " + id)));
    }

    @Override
    public Page<MedicionResponseDTO> listarPorAsociacion(Integer idAsociacion, Pageable pageable) {
        return medicionRepository.findByAsociacion_IdAsociacionOrderByFechaDesc(idAsociacion, pageable)
                .map(MedicionMapper::toResponseDTO);
    }

    @Override
    public Page<MedicionResponseDTO> listarPorRango(Integer idAsociacion, TipoMedicion tipo,
                                                     LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        return medicionRepository
                .findByAsociacion_IdAsociacionAndTipoMedicionAndFechaBetween(idAsociacion, tipo, desde, hasta, pageable)
                .map(MedicionMapper::toResponseDTO);
    }

    @Override
    public MedicionResponseDTO obtenerUltima(Integer idAsociacion) {
        return medicionRepository.findTopByAsociacion_IdAsociacionOrderByFechaDesc(idAsociacion)
                .map(MedicionMapper::toResponseDTO)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La asociacion " + idAsociacion + " todavia no tiene mediciones registradas"));
    }

    private void validarCoherenciaTipo(TipoMedicion tipoSensor, TipoMedicion tipoMedicion) {
        if (tipoSensor != tipoMedicion) {
            throw new ReglaDeNegocioException(
                    "El tipo de medicion (" + tipoMedicion + ") no coincide con el tipo del sensor asociado (" + tipoSensor + ")");
        }
    }

    private void validarCoherenciaOrigen(boolean esSimulado, OrigenMedicion origen) {
        boolean coincide = (esSimulado && origen == OrigenMedicion.SIMULADO)
                || (!esSimulado && origen == OrigenMedicion.REAL);
        if (!coincide) {
            throw new ReglaDeNegocioException(
                    "El origen de la medicion (" + origen + ") no coincide con el tipo de sensor (simulado=" + esSimulado + ")");
        }
    }
}
