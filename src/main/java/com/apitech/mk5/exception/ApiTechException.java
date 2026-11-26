package com.apitech.mk5.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepcion base de todas las excepciones propias del dominio de
 * ApiTech. Es {@code abstract} a proposito: nunca se lanza directamente
 * una {@code ApiTechException}, siempre una de sus subclases concretas
 * ({@link RecursoNoEncontradoException}, {@link ReglaDeNegocioException},
 * {@link ValidacionException}, {@link AccesoNoAutorizadoException}).
 *
 * <p>Extiende {@link RuntimeException} (no una excepcion verificada / 
 * {@code checked}) de forma deliberada: si fuera verificada, cada
 * metodo de cada {@code service} tendria que declarar
 * {@code throws ApiTechException} o envolverla, lo cual ensuciaria
 * firmas de metodos en toda la capa de negocio sin aportar seguridad
 * real -- Spring ya intercepta estas excepciones de forma centralizada
 * (ver {@link GlobalExceptionHandler}), asi que no hace falta forzar al
 * compilador a recordarlo en cada firma.</p>
 *
 * <p>El metodo abstracto {@link #getHttpStatus()} es la pieza que
 * permite que {@link GlobalExceptionHandler} traduzca CUALQUIER
 * excepcion de esta jerarquia a la respuesta HTTP correcta con un solo
 * metodo {@code @ExceptionHandler(ApiTechException.class)}, sin
 * necesidad de un manejador separado por cada subclase.</p>
 */
public abstract class ApiTechException extends RuntimeException {

    protected ApiTechException(String message) {
        super(message);
    }

    protected ApiTechException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Codigo de estado HTTP que corresponde a este tipo de error. Cada
     * subclase concreta decide el suyo (por ejemplo, 404 para "no
     * encontrado", 409 para "conflicto de regla de negocio").
     */
    public abstract HttpStatus getHttpStatus();
}
