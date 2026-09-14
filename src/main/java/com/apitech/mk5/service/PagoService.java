package com.apitech.mk5.service;

import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.suscripcion.Pago;
import com.apitech.mk5.entity.suscripcion.Suscripcion;
import com.apitech.mk5.entity.suscripcion.PlanSuscripcion;

import java.math.BigDecimal;
import java.util.List;

/** Servicio de casos de uso de planes, suscripciones y pagos. */
public interface PagoService {
    List<PlanSuscripcion> planesActivos();
    List<Pago> pagosEmpresa(Integer idEmpresa);
    List<Suscripcion> suscripcionesEmpresa(Integer idEmpresa);
    List<Pago> pagosPendientes();
    Pago checkout(Empresa empresa, Integer idPlan, String metodo, String referencia);
    void verificar(Integer id);
    void verificar(Integer id, Integer idUsuarioVerifico);
    void rechazar(Integer id);
    BigDecimal totalPagos();
}
