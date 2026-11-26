package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.MedicionRequestDTO;
import com.apitech.mk5.entity.apiario.*;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.repository.apiario.MedicionRepository;
import com.apitech.mk5.repository.apiario.MonitoreoRepository;
import com.apitech.mk5.repository.apiario.SensorColmenaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link MedicionService}.
 *
 * <p>Valida las tres reglas del trigger
 * {@code trg_mediciones_coherencia_bi} implementadas en Java:
 * monitoreo activo, tipo de medición coherente y origen coherente.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicionServiceImpl — pruebas unitarias")
class MedicionServiceTest {

    @Mock private MedicionRepository medicionRepository;
    @Mock private SensorColmenaRepository sensorColmenaRepository;
    @Mock private MonitoreoRepository monitoreoRepository;

    @InjectMocks
    private com.apitech.mk5.service.impl.MedicionServiceImpl medicionService;

    private SensorColmena asociacionActiva;
    private Sensor sensorSimuladoTemp;

    @BeforeEach
    void setUp() {
        Empresa empresa = new Empresa(
                "900-1", "El Panal", null, null, null, "activa");
        Colmena colmena = new Colmena(empresa, null, "C001", "Sector A", "Estable");

        sensorSimuladoTemp = new Sensor(
                empresa, "SIM-TEMP-001", TipoMedicion.TEMPERATURA,
                null, null, true, "activo");

        asociacionActiva = new SensorColmena(sensorSimuladoTemp, colmena, 1);
    }

    // ── Test 1: Sin monitoreo activo → excepción ────────────────
    @Test
    @DisplayName("Sin monitoreo activo debe lanzar ReglaDeNegocioException")
    void sinMonitoreoActivo_lanzaExcepcion() {
        MedicionRequestDTO dto = new MedicionRequestDTO(
                1, TipoMedicion.TEMPERATURA, 35.5f, "°C", OrigenMedicion.SIMULADO);

        when(sensorColmenaRepository.findById(1))
                .thenReturn(Optional.of(asociacionActiva));
        when(monitoreoRepository.existsByAsociacion_IdAsociacionAndEstado(1, "activo"))
                .thenReturn(false);

        assertThatThrownBy(() -> medicionService.registrar(dto))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("monitoreo activo");
    }

    // ── Test 2: Tipo de medición no coincide → excepción ────────
    @Test
    @DisplayName("Tipo de medición incorrecto debe lanzar ReglaDeNegocioException")
    void tipoMedicion_incorrecto_lanzaExcepcion() {
        // Sensor es TEMPERATURA pero se intenta registrar HUMEDAD
        MedicionRequestDTO dto = new MedicionRequestDTO(
                1, TipoMedicion.HUMEDAD, 65f, "%", OrigenMedicion.SIMULADO);

        when(sensorColmenaRepository.findById(1))
                .thenReturn(Optional.of(asociacionActiva));
        when(monitoreoRepository.existsByAsociacion_IdAsociacionAndEstado(1, "activo"))
                .thenReturn(true);

        assertThatThrownBy(() -> medicionService.registrar(dto))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("tipo");
    }

    // ── Test 3: Origen incorrecto → excepción ───────────────────
    @Test
    @DisplayName("Origen REAL en sensor simulado debe lanzar ReglaDeNegocioException")
    void origen_incorrecto_lanzaExcepcion() {
        // Sensor es simulado pero se intenta registrar como REAL
        MedicionRequestDTO dto = new MedicionRequestDTO(
                1, TipoMedicion.TEMPERATURA, 35.5f, "°C", OrigenMedicion.REAL);

        when(sensorColmenaRepository.findById(1))
                .thenReturn(Optional.of(asociacionActiva));
        when(monitoreoRepository.existsByAsociacion_IdAsociacionAndEstado(1, "activo"))
                .thenReturn(true);

        assertThatThrownBy(() -> medicionService.registrar(dto))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("origen");
    }

    // ── Test 4: Registro exitoso ─────────────────────────────────
    @Test
    @DisplayName("Medición correcta debe guardarse exitosamente")
    void medicion_correcta_seGuarda() {
        MedicionRequestDTO dto = new MedicionRequestDTO(
                1, TipoMedicion.TEMPERATURA, 35.5f, "°C", OrigenMedicion.SIMULADO);

        when(sensorColmenaRepository.findById(1))
                .thenReturn(Optional.of(asociacionActiva));
        when(monitoreoRepository.existsByAsociacion_IdAsociacionAndEstado(1, "activo"))
                .thenReturn(true);
        when(medicionRepository.save(any(Medicion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var resultado = medicionService.registrar(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.tipoMedicion()).isEqualTo(TipoMedicion.TEMPERATURA);
        assertThat(resultado.valor()).isEqualTo(35.5f);
        verify(medicionRepository).save(any(Medicion.class));
    }
}