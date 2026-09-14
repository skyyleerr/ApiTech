package com.apitech.mk5.dto.response;

import java.io.Serializable;
import java.util.List;

public record ImportacionResultadoDTO(
        List<FilaImportacionDTO> filas,
        int procesadas,
        int validas,
        int errores,
        int duplicados,
        boolean confirmada) implements Serializable {
    public record FilaImportacionDTO(
            int numero,
            String fecha,
            String idColmena,
            String idAsociacion,
            String tipo,
            String valor,
            String unidad,
            String origen,
            String produccion,
            String observaciones,
            String error,
            boolean valida,
            boolean duplicada) implements Serializable { }
}
