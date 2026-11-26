package com.apitech.mk5.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un usuario autenticado intenta acceder o modificar un
 * recurso que no le corresponde -- el caso central en un sistema
 * multi-tenant como ApiTech: un usuario de la empresa A intentando ver
 * o editar datos de la empresa B.
 *
 * <p>Se usara sobre todo desde la etapa de {@code service} del nucleo
 * apicola, verificando que el {@code idEmpresa} del recurso solicitado
 * coincida con el de la empresa del usuario autenticado -- una vez que
 * la capa de seguridad (pendiente, etapa futura) este disponible para
 * saber quien es "el usuario autenticado".</p>
 */
public class AccesoNoAutorizadoException extends ApiTechException {

    public AccesoNoAutorizadoException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
