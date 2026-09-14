package com.apitech.mk5.service;

import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.suscripcion.Suscripcion;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.repository.suscripcion.SuscripcionRepository;
import com.apitech.mk5.security.UsuarioContexto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Reglas transversales de operación de una empresa cliente:
 * estado de la cuenta y límites del plan.
 */
@Service
public class EmpresaOperacionService {
    public static final int LIMITE_BASICO = 20;
    public static final int LIMITE_PROFESIONAL = 60;

    private final EmpresaRepository empresas;
    private final SuscripcionRepository suscripciones;
    private final UsuarioContexto contexto;

    public EmpresaOperacionService(EmpresaRepository empresas,
                                   SuscripcionRepository suscripciones,
                                   UsuarioContexto contexto) {
        this.empresas = empresas;
        this.suscripciones = suscripciones;
        this.contexto = contexto;
    }

    /** Solo bloquea a clientes. El equipo interno ApiTech no depende del estado de una empresa. */
    public void validarPuedeOperar(Integer idEmpresa) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) return;
        Integer actual = contexto.getIdEmpresaActual();
        if (actual == null) return;
        if (!actual.equals(idEmpresa)) {
            throw new ReglaDeNegocioException("No puedes operar con datos de otra empresa");
        }
        Empresa empresa = empresas.findById(idEmpresa)
                .orElseThrow(() -> new ReglaDeNegocioException("La empresa no existe"));
        if (!"activa".equalsIgnoreCase(empresa.getEstado())) {
            throw new ReglaDeNegocioException("La empresa está pausada por ApiTech. No se permiten cambios hasta reactivarla.");
        }
        if (suscripcionVencida(idEmpresa)) {
            throw new ReglaDeNegocioException("La suscripción de la empresa está vencida. No se permiten operaciones hasta renovarla."); 
        }
    }

    public boolean estaPausada(Integer idEmpresa) {
        return idEmpresa != null && empresas.findById(idEmpresa)
                .map(e -> !"activa".equalsIgnoreCase(e.getEstado()))
                .orElse(false);
    }

    public int limiteColmenas(Integer idEmpresa) {
        return limitePlan(idEmpresa);
    }

    public int limiteSensores(Integer idEmpresa) {
        return limitePlan(idEmpresa);
    }

    private int limitePlan(Integer idEmpresa) {
        if (idEmpresa == null) return Integer.MAX_VALUE;
        return suscripciones.findFirstByEmpresa_IdEmpresaAndEstadoOrderByIdSuscripcionDesc(
                        idEmpresa, Suscripcion.EstadoSuscripcion.ACTIVA)
                .map(Suscripcion::getPlan)
                .map(p -> p.getNombrePlan().toLowerCase().contains("profesional")
                        ? LIMITE_PROFESIONAL : LIMITE_BASICO)
                .orElse(0);
    }

    public void validarLimiteColmenas(Integer idEmpresa, long actuales) {
        validarPuedeOperar(idEmpresa);
        int limite = limiteColmenas(idEmpresa);
        if (actuales >= limite) {
            throw new ReglaDeNegocioException(
                    "Has alcanzado el límite de " + limite + " colmenas de tu plan.");
        }
    }

    public void validarLimiteSensores(Integer idEmpresa, long actuales) {
        validarPuedeOperar(idEmpresa);
        int limite = limiteSensores(idEmpresa);
        if (actuales >= limite) {
            throw new ReglaDeNegocioException(
                    "Has alcanzado el límite de " + limite + " sensores de tu plan.");
        }
    }

    public Suscripcion suscripcionActual(Integer idEmpresa) {
        return suscripciones.findFirstByEmpresa_IdEmpresaAndEstadoOrderByIdSuscripcionDesc(
                idEmpresa, Suscripcion.EstadoSuscripcion.ACTIVA).orElse(null);
    }

    public boolean suscripcionVencida(Integer idEmpresa) {
        Suscripcion s = suscripcionActual(idEmpresa);
        return s == null || s.getFechaVencimiento() == null
                || s.getFechaVencimiento().isBefore(LocalDate.now());
    }
}
