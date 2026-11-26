package com.apitech.mk5.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando se solicita un recurso por su id (u otro
 * identificador unico) y no existe -- por ejemplo, buscar una
 * {@code Colmena} con un {@code idColmena} que no corresponde a
 * ninguna fila.
 *
 * <p>Uso tipico en un {@code service}:</p>
 * <pre>{@code
 * Colmena colmena = colmenaRepository.findById(id)
 *     .orElseThrow(() -> new RecursoNoEncontradoException(
 *         "No existe una colmena con id " + id));
 * }</pre>
 */
public class RecursoNoEncontradoException extends ApiTechException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
