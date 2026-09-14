package com.apitech.mk5.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza para errores de validacion que Bean Validation
 * ({@code @NotNull}, {@code @Size}, etc. sobre un DTO) no puede
 * expresar por si sola -- tipicamente porque involucra comparar dos
 * campos entre si, o una regla que depende de mas de un valor a la vez.
 *
 * <p>Ejemplo concreto que usaremos mas adelante: {@code RangoUmbral}
 * exige {@code valorMin < valorMax} (hoy en {@code trg_umbral_bi/bu}).
 * Una anotacion como {@code @Min}/{@code @Max} no puede comparar dos
 * campos entre si, asi que esta regla se valida explicitamente en el
 * {@code service} (o en un validador de clase en el paquete
 * {@code validation}) y, si falla, se lanza esta excepcion.</p>
 *
 * <p>Nota: los errores de Bean Validation disparados automaticamente
 * por {@code @Valid} sobre un DTO NO pasan por esta clase -- Spring los
 * reporta como {@code MethodArgumentNotValidException}, que
 * {@link GlobalExceptionHandler} maneja por separado (ver esa clase).
 * {@code ValidacionException} es para las reglas que el propio
 * {@code service} detecta manualmente.</p>
 */
public class ValidacionException extends ApiTechException {

    public ValidacionException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
