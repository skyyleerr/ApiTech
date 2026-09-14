package com.apitech.mk5.entity.apiario;

/**
 * Origen de una medicion, reflejando el {@code ENUM('SIMULADO','REAL')}
 * de la columna {@code mediciones.origen}.
 *
 * <p>Debe coincidir con {@link Sensor#isEsSimulado()}: un sensor
 * simulado ({@code esSimulado = true}) solo puede producir mediciones
 * {@code SIMULADO}; un sensor fisico ({@code esSimulado = false}) solo
 * puede producir mediciones {@code REAL}. Esa coherencia la impone hoy
 * el trigger {@code trg_mediciones_coherencia_bi/bu} -- se reforzara en
 * el {@code service} de {@link Medicion} en una etapa posterior.</p>
 */
public enum OrigenMedicion {
    SIMULADO,
    REAL
}
