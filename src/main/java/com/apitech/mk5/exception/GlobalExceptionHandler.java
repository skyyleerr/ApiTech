package com.apitech.mk5.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Manejador centralizado de excepciones para toda la API REST.
 *
 * <p>{@code @RestControllerAdvice} hace que los metodos de esta clase
 * intercepten las excepciones lanzadas por CUALQUIER
 * {@code @RestController} del proyecto -- de aqui en adelante, ningun
 * controller necesita un {@code try/catch}: basta con que el
 * {@code service} lance la excepcion adecuada y esta clase se encarga
 * de traducirla a una respuesta HTTP con el formato de
 * {@link ErrorResponse}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja CUALQUIER excepcion de la jerarquia {@link ApiTechException}
     * (recurso no encontrado, regla de negocio, validacion manual,
     * acceso no autorizado) con un solo metodo: cada subclase ya sabe,
     * a traves de {@link ApiTechException#getHttpStatus()}, que codigo
     * HTTP le corresponde.
     */
    @ExceptionHandler(ApiTechException.class)
    public ResponseEntity<ErrorResponse> manejarApiTechException(ApiTechException ex) {
        HttpStatus status = ex.getHttpStatus();
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Maneja los errores que dispara automaticamente Spring cuando un
     * DTO anotado con {@code @Valid} no cumple sus anotaciones de Bean
     * Validation ({@code @NotNull}, {@code @Size}, {@code @Email},
     * etc.). En vez de devolver el formato generico de Spring, se
     * reempaqueta en el mismo {@link ErrorResponse} que usa el resto de
     * la API, juntando TODOS los campos invalidos en {@code detalles}
     * de una sola vez (en vez de que el cliente tenga que corregir un
     * campo, reintentar, y descubrir el siguiente error uno por uno).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionDTO(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatearErrorDeCampo)
                .toList();

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Uno o mas campos no son validos",
                detalles
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(AuthorizationDeniedException ex) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase(),
                "No tiene permisos para realizar esta operación.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Maneja las peticiones a rutas que no corresponden a ningun
     * controller ni recurso estatico (por ejemplo, entrar a
     * {@code http://localhost:8080/} sin que exista un endpoint para
     * la raiz). Spring lanza {@link NoResourceFoundException}
     * internamente en este caso -- SIN este manejador especifico,
     * caeria en {@link #manejarErrorInesperado} y se reportaria como
     * un 500 (error del servidor) en vez de un 404 (recurso no
     * encontrado), que es el codigo correcto para "esta URL no existe".
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRutaNoEncontrada(NoResourceFoundException ex) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                "La ruta solicitada no existe. Consulte /api/... para los endpoints disponibles.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Falta un {@code @RequestParam} obligatorio (por ejemplo,
     * {@code GET /api/colmenas} sin {@code idEmpresa}). Sin este
     * manejador especifico, caeria en {@link #manejarErrorInesperado}
     * y se reportaria como 500 en vez de 400 -- el mismo problema que
     * {@link #manejarRutaNoEncontrada}, pero para parametros faltantes.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> manejarParametroFaltante(MissingServletRequestParameterException ex) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Falta el parametro obligatorio: " + ex.getParameterName());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Un parametro llego con un valor que no se puede convertir al
     * tipo esperado (por ejemplo, {@code ?idEmpresa=abc} cuando se
     * espera un numero entero).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El parametro '" + ex.getName() + "' tiene un valor invalido: " + ex.getValue());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * El cuerpo de la peticion no es un JSON valido, o no coincide con
     * la forma esperada del DTO (por ejemplo, un tipo de dato incorrecto
     * dentro del JSON).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El cuerpo de la peticion no es un JSON valido o no tiene la forma esperada");
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Red de seguridad final: cualquier excepcion NO prevista (por
     * ejemplo, un {@code NullPointerException} por un error de
     * programacion) se convierte en un 500 con un mensaje generico --
     * nunca se expone el mensaje interno real ni el stack trace al
     * cliente de la API, para no filtrar detalles internos del
     * sistema. El detalle completo sigue quedando disponible en los
     * logs del servidor (Spring lo registra automaticamente).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorInesperado(Exception ex) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocurrio un error inesperado. Contacte al administrador si el problema persiste."
        );
        return ResponseEntity.internalServerError().body(body);
    }

    private String formatearErrorDeCampo(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
