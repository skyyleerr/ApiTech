package com.apitech.mk5.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando una operacion es tecnicamente valida (los datos
 * tienen el formato correcto) pero viola una regla de negocio del
 * dominio -- el equivalente en Java de un {@code SIGNAL SQLSTATE
 * '45000'} en los triggers del esquema SQL.
 *
 * <p>Ejemplos concretos que usaremos en la etapa de {@code service}:</p>
 * <ul>
 *     <li>Intentar activar un {@code Monitoreo} cuando el sensor esta
 *         inactivo (hoy: {@code trg_monitoreo_ins/upd}).</li>
 *     <li>Intentar crear una {@code SensorColmena} para un sensor que
 *         ya tiene una asociacion activa.</li>
 *     <li>Intentar asignar a un usuario con rol {@code Admin_ApiTech}
 *         una empresa (hoy: {@code trg_usuarios_empresa_ins/upd}).</li>
 * </ul>
 *
 * <p>Se traduce a HTTP 409 (Conflict): la peticion esta bien formada,
 * pero entra en conflicto con el estado actual de los datos o con una
 * regla del dominio -- distinto de un 400 (que indicaria un problema en
 * la forma misma de la peticion).</p>
 */
public class ReglaDeNegocioException extends ApiTechException {

    public ReglaDeNegocioException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
