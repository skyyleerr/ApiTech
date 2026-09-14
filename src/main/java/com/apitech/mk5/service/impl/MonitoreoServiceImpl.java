package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.response.MonitoreoResponseDTO;
import com.apitech.mk5.entity.apiario.Colmena;
import com.apitech.mk5.entity.apiario.Monitoreo;
import com.apitech.mk5.entity.apiario.Sensor;
import com.apitech.mk5.entity.apiario.SensorColmena;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.mapper.MonitoreoMapper;
import com.apitech.mk5.repository.apiario.MonitoreoRepository;
import com.apitech.mk5.repository.apiario.SensorColmenaRepository;
import com.apitech.mk5.service.MonitoreoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementacion de {@link MonitoreoService}.
 *
 * <p><b>Convencion de nombres de estado:</b> el esquema SQL solo define
 * {@code 'activo'} como valor por defecto de {@code monitoreo.estado},
 * sin definir explicitamente el valor opuesto. Este proyecto adopta
 * {@code "inactivo"} como convencion para un monitoreo detenido -- se
 * documenta aqui porque no viene dictado por la base de datos, es una
 * decision de la aplicacion.</p>
 */
@Service
@Transactional(readOnly = true)
public class MonitoreoServiceImpl implements MonitoreoService {

    private static final String ESTADO_ACTIVO = "activo";
    private static final String ESTADO_INACTIVO = "inactivo";
    private static final String COLMENA_ESTABLE = "Estable";

    private final MonitoreoRepository monitoreoRepository;
    private final SensorColmenaRepository sensorColmenaRepository;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;

    public MonitoreoServiceImpl(MonitoreoRepository monitoreoRepository,
                                 SensorColmenaRepository sensorColmenaRepository,
                                 com.apitech.mk5.service.EmpresaOperacionService empresaOperacion) {
        this.monitoreoRepository = monitoreoRepository;
        this.sensorColmenaRepository = sensorColmenaRepository;
        this.empresaOperacion = empresaOperacion;
    }

    @Override
    @Transactional
    public MonitoreoResponseDTO activar(Integer idAsociacion) {
        SensorColmena asociacion = sensorColmenaRepository.findById(idAsociacion)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una asociacion sensor-colmena con id " + idAsociacion));

        empresaOperacion.validarPuedeOperar(asociacion.getIdEmpresa());

        if (monitoreoRepository.existsByAsociacion_IdAsociacionAndEstado(idAsociacion, ESTADO_ACTIVO)) {
            throw new ReglaDeNegocioException(
                    "Ya existe un monitoreo activo para la asociacion " + idAsociacion);
        }

        validarCondicionesParaMonitorear(asociacion);

        Monitoreo monitoreo = new Monitoreo(asociacion, ESTADO_ACTIVO);
        monitoreoRepository.save(monitoreo);
        return MonitoreoMapper.toResponseDTO(monitoreo);
    }

    @Override
    @Transactional
    public MonitoreoResponseDTO desactivar(Integer idMonitoreo) {
        Monitoreo monitoreo = buscarOFallar(idMonitoreo);
        empresaOperacion.validarPuedeOperar(monitoreo.getAsociacion().getIdEmpresa());
        monitoreo.setEstado(ESTADO_INACTIVO);
        monitoreo.setFechaFin(LocalDateTime.now());
        return MonitoreoMapper.toResponseDTO(monitoreo);
    }

    @Override
    public MonitoreoResponseDTO obtenerPorId(Integer id) {
        return MonitoreoMapper.toResponseDTO(buscarOFallar(id));
    }

    @Override
    public List<MonitoreoResponseDTO> listarPorAsociacion(Integer idAsociacion) {
        return monitoreoRepository.findByAsociacion_IdAsociacion(idAsociacion).stream()
                .map(MonitoreoMapper::toResponseDTO)
                .toList();
    }

    /**
     * Replica {@code trg_monitoreo_ins/upd} + {@code trg_monitoreo_empresa_bi/bu}:
     * exige asociacion activa, sensor activo, y colmena en estado
     * "Estable" antes de permitir activar un monitoreo.
     */
    private void validarCondicionesParaMonitorear(SensorColmena asociacion) {
        if (!asociacion.isActivo()) {
            throw new ReglaDeNegocioException(
                    "No se puede activar el monitoreo: la asociacion sensor-colmena no esta activa");
        }
        Sensor sensor = asociacion.getSensor();
        if (!ESTADO_ACTIVO.equals(sensor.getEstado())) {
            throw new ReglaDeNegocioException(
                    "No se puede activar el monitoreo: el sensor " + sensor.getCodigo() + " no esta activo");
        }
        Colmena colmena = asociacion.getColmena();
        if (!COLMENA_ESTABLE.equals(colmena.getEstado())) {
            throw new ReglaDeNegocioException(
                    "No se puede activar el monitoreo: la colmena " + colmena.getNombre() + " no esta en estado Estable");
        }
    }

    private Monitoreo buscarOFallar(Integer id) {
        return monitoreoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un monitoreo con id " + id));
    }
}
