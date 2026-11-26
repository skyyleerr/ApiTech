package com.apitech.mk5.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Forma JSON unica y consistente de cualquier error devuelto por la
 * API, sin importar si el error viene de una {@link ApiTechException},
 * de una validacion de Bean Validation, o de un fallo inesperado.
 *
 * <p>Se implementa como {@code record} (a diferencia de las entidades
 * JPA del proyecto, que son clases mutables con getters/setters) porque
 * aqui no aplican las mismas razones: un {@code ErrorResponse} no lo
 * gestiona Hibernate, no tiene ciclo de vida, no necesita un
 * constructor vacio ni setters -- es un dato de transporte que se
 * crea completo de una vez y nunca se modifica despues. Un
 * {@code record} expresa esa inmutabilidad directamente en el tipo,
 * con menos codigo repetitivo que una clase tradicional.</p>
 *
 * @param timestamp momento exacto en que se genero el error.
 * @param status    codigo de estado HTTP numerico (por ejemplo 404).
 * @param error     nombre corto del estado HTTP (por ejemplo "Not Found").
 * @param message   mensaje principal, legible, del error.
 * @param detalles  lista opcional de mensajes adicionales -- se usa
 *                  sobre todo cuando el error viene de Bean Validation
 *                  sobre un DTO con varios campos invalidos a la vez;
 *                  {@code null} cuando no aplica (no una lista vacia,
 *                  para distinguir claramente "no hay detalles" de
 *                  "hay una lista de detalles, pero esta vacia").
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> detalles
) {

    /**
     * Fabrica de conveniencia para el caso mas comun: un error sin
     * lista de detalles adicionales.
     */
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, null);
    }

    /**
     * Fabrica de conveniencia para errores de validacion con varios
     * mensajes (uno por campo invalido).
     */
    public static ErrorResponse of(int status, String error, String message, List<String> detalles) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, detalles);
    }
}
