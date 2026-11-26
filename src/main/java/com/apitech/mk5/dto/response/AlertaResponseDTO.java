package com.apitech.mk5.dto.response;

import java.time.LocalDateTime;

public record AlertaResponseDTO(
        Integer idAlerta,
        Integer idColmena,
        Integer idEmpresa,
        Long idMedicion,
        Integer idSensor,
        String tipo,
        String descripcion,
        Float valorDetectado,
        Float umbralMin,
        Float umbralMax,
        String severidad,
        String estado,
        LocalDateTime fecha,
        LocalDateTime fechaResolucion,
        UsuarioResumenDTO usuarioResolucion
) {
}
