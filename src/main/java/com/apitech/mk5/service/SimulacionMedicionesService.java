package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.MedicionRequestDTO;
import com.apitech.mk5.dto.response.MedicionResponseDTO;
import com.apitech.mk5.entity.apiario.*;
import com.apitech.mk5.entity.evento.Notificacion;
import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.repository.apiario.*;
import com.apitech.mk5.repository.evento.NotificacionRepository;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Motor de simulacion de sensores.
 *
 * Cuando existe una asociacion activa con un sensor simulado, el servicio
 * garantiza el monitoreo activo y genera una lectura cada 30 segundos.
 * Temperatura y humedad fluctuan alrededor del ultimo valor; el peso solo
 * aumenta, simulando la acumulacion de miel.
 *
 * Cada lectura se compara contra el umbral especifico de la empresa y,
 * si no existe, contra el umbral global. Si se sale del rango se crea una
 * alerta y se genera una notificacion para los usuarios de esa empresa.
 */
@Service
public class SimulacionMedicionesService {
    private static final String ACTIVO = "activo";

    private final SensorColmenaRepository asociaciones;
    private final MonitoreoRepository monitoreos;
    private final MedicionRepository mediciones;
    private final MedicionService medicionService;
    private final MonitoreoService monitoreoService;
    private final RangoUmbralRepository umbrales;
    private final AlertaRepository alertas;
    private final NotificacionRepository notificaciones;
    private final UsuarioRepository usuarios;
    private final EmpresaOperacionService empresaOperacion;

    public SimulacionMedicionesService(SensorColmenaRepository asociaciones,
                                       MonitoreoRepository monitoreos,
                                       MedicionRepository mediciones,
                                       MedicionService medicionService,
                                       MonitoreoService monitoreoService,
                                       RangoUmbralRepository umbrales,
                                       AlertaRepository alertas,
                                       NotificacionRepository notificaciones,
                                       UsuarioRepository usuarios,
                                       EmpresaOperacionService empresaOperacion) {
        this.asociaciones = asociaciones;
        this.monitoreos = monitoreos;
        this.mediciones = mediciones;
        this.medicionService = medicionService;
        this.monitoreoService = monitoreoService;
        this.umbrales = umbrales;
        this.alertas = alertas;
        this.notificaciones = notificaciones;
        this.usuarios = usuarios;
        this.empresaOperacion = empresaOperacion;
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    @Transactional
    public void generarLecturas() {
        asociaciones.findByActivoTrue().stream()
                .filter(a -> a.getSensor().isEsSimulado())
                .filter(a -> ACTIVO.equalsIgnoreCase(a.getSensor().getEstado()))
                .filter(a -> !empresaOperacion.estaPausada(a.getIdEmpresa()))
                .filter(a -> !empresaOperacion.suscripcionVencida(a.getIdEmpresa()))
                .forEach(this::generarParaAsociacion);
    }

    private void generarParaAsociacion(SensorColmena asociacion) {
        Integer idAsociacion = asociacion.getIdAsociacion();

        if (!monitoreos.existsByAsociacion_IdAsociacionAndEstado(idAsociacion, ACTIVO)) {
            try {
                monitoreoService.activar(idAsociacion);
            } catch (RuntimeException ignored) {
                return;
            }
        }

        TipoMedicion tipo = asociacion.getSensor().getTipo();
        float valor = siguienteValor(asociacion, tipo);
        String unidad = switch (tipo) {
            case TEMPERATURA -> "°C";
            case HUMEDAD -> "%";
            case PESO -> "kg";
        };

        MedicionResponseDTO creada = medicionService.registrar(
                new MedicionRequestDTO(idAsociacion, tipo, valor, unidad, OrigenMedicion.SIMULADO));

        mediciones.findById(creada.idMedicion()).ifPresent(m -> evaluarUmbral(m, asociacion));
    }

    private void evaluarUmbral(Medicion medicion, SensorColmena asociacion) {
        Integer empresa = asociacion.getIdEmpresa();
        TipoMedicion tipo = medicion.getTipoMedicion();

        RangoUmbral umbral = umbrales.findByEmpresa_IdEmpresaAndTipoMedicion(empresa, tipo)
                .orElseGet(() -> umbrales.findByEmpresaIsNullAndTipoMedicion(tipo).orElse(null));

        // Si la empresa todavia no ha configurado un umbral, usamos un
        // limite seguro de demostracion para que el flujo de alertas funcione.
        if (umbral == null) {
            umbral = umbralPorDefecto(tipo);
        }

        float valor = medicion.getValor();
        if (valor >= umbral.getValorMin() && valor <= umbral.getValorMax()) {
            return;
        }

        String tipoAlerta = tipo.name() + "_FUERA_RANGO";
        if (alertas.existsByIdEmpresaAndColmena_IdColmenaAndTipoAndEstado(
                empresa, asociacion.getColmena().getIdColmena(), tipoAlerta, "ACTIVA")) {
            return;
        }

        float distancia = valor < umbral.getValorMin()
                ? umbral.getValorMin() - valor
                : valor - umbral.getValorMax();
        float rango = Math.max(1f, umbral.getValorMax() - umbral.getValorMin());
        String severidad = distancia > rango * 0.20f ? "critica" : "advertencia";

        Alerta alerta = new Alerta(
                asociacion.getColmena(),
                empresa,
                medicion,
                asociacion.getSensor(),
                tipoAlerta,
                "El valor de " + tipo.name().toLowerCase()
                        + " (" + valor + " " + medicion.getUnidad()
                        + ") esta fuera del rango permitido.",
                valor,
                umbral.getValorMin(),
                umbral.getValorMax(),
                severidad,
                "ACTIVA");

        alertas.save(alerta);

        List<Usuario> destinatarios = usuarios.findByEmpresa_IdEmpresa(empresa);
        for (Usuario usuario : destinatarios) {
            Notificacion n = new Notificacion(
                    usuario,
                    asociacion.getColmena().getEmpresa(),
                    "ALERTA",
                    "Alerta en " + asociacion.getColmena().getNombre(),
                    alerta.getDescripcion(),
                    false,
                    alerta,
                    null);
            notificaciones.save(n);
        }
    }

    private RangoUmbral umbralPorDefecto(TipoMedicion tipo) {
        return switch (tipo) {
            case TEMPERATURA -> new RangoUmbral(null, tipo, 18f, 35f, "°C", "activo");
            case HUMEDAD -> new RangoUmbral(null, tipo, 35f, 80f, "%", "activo");
            case PESO -> new RangoUmbral(null, tipo, 0f, 80f, "kg", "activo");
        };
    }

    private float siguienteValor(SensorColmena asociacion, TipoMedicion tipo) {
        float ultimo = mediciones
                .findTopByAsociacion_IdAsociacionOrderByFechaDesc(asociacion.getIdAsociacion())
                .map(Medicion::getValor)
                .orElseGet(() -> switch (tipo) {
                    case TEMPERATURA -> 30.0f;
                    case HUMEDAD -> 62.0f;
                    case PESO -> 18.0f;
                });

        return switch (tipo) {
            case TEMPERATURA -> limitar(ultimo + aleatorio(-1.5f, 1.5f), 10f, 45f);
            case HUMEDAD -> limitar(ultimo + aleatorio(-3f, 3f), 10f, 95f);
            case PESO -> limitar(ultimo + aleatorio(0.05f, 0.35f), 0f, 100f);
        };
    }

    private float aleatorio(float minimo, float maximo) {
        return (float) ThreadLocalRandom.current().nextDouble(minimo, maximo);
    }

    private float limitar(float valor, float minimo, float maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }
}
