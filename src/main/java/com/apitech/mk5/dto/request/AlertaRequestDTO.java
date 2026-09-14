package com.apitech.mk5.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code idMedicion} e {@code idSensor} son opcionales (una alerta
 * puede generarse sin una lectura puntual asociada). {@code idEmpresa}
 * NO se pide aqui: el {@code service} la deriva de {@code idColmena},
 * igual que hace el trigger {@code trg_alertas_empresa_bi}.
 */
public record AlertaRequestDTO(

        @NotNull(message = "La colmena es obligatoria")
        Integer idColmena,

        Long idMedicion,

        Integer idSensor,

        @NotBlank(message = "El tipo de alerta es obligatorio")
        @Size(max = 100, message = "El tipo no puede superar 100 caracteres")
        String tipo,

        String descripcion,

        Float valorDetectado,

        Float umbralMin,

        Float umbralMax,

        @NotBlank(message = "La severidad es obligatoria")
        @Size(max = 20, message = "La severidad no puede superar 20 caracteres")
        String severidad
) {
}
