package com.apitech.mk5.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para crear una asociacion sensor-colmena nueva. No incluye
 * {@code idEmpresa}: el {@code service} lo deriva automaticamente del
 * sensor (y valida que coincida con el de la colmena), replicando lo
 * que hoy hacen las FK compuestas del esquema SQL.
 */
public record SensorColmenaRequestDTO(

        @NotNull(message = "El sensor es obligatorio")
        Integer idSensor,

        @NotNull(message = "La colmena es obligatoria")
        Integer idColmena
) {
}
