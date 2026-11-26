package com.apitech.mk5.service.impl;

import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.suscripcion.Pago;
import com.apitech.mk5.entity.suscripcion.PlanSuscripcion;
import com.apitech.mk5.entity.suscripcion.Suscripcion;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.repository.suscripcion.PagoRepository;
import com.apitech.mk5.repository.suscripcion.PlanSuscripcionRepository;
import com.apitech.mk5.repository.suscripcion.SuscripcionRepository;
import com.apitech.mk5.service.PagoService;
import com.apitech.mk5.service.PagoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementación de los casos de uso de planes, suscripciones y pagos.
 *
 * <p>El pago se registra como una solicitud pendiente. La activación de la
 * suscripción ocurre únicamente cuando personal ApiTech verifica el pago.
 * Esta clase replica las reglas de coherencia y unicidad definidas en el
 * esquema MySQL, sin delegar la seguridad únicamente en la base de datos.</p>
 */
@Service
@Transactional(readOnly = true)
public class PagoServiceImpl implements PagoService {

    private final PlanSuscripcionRepository planes;
    private final SuscripcionRepository suscripciones;
    private final PagoRepository pagos;

    public PagoServiceImpl(PlanSuscripcionRepository planes,
                           SuscripcionRepository suscripciones,
                           PagoRepository pagos) {
        this.planes = planes;
        this.suscripciones = suscripciones;
        this.pagos = pagos;
    }

    @Override
    public List<PlanSuscripcion> planesActivos() {
        // Solo estos dos planes forman parte de la oferta comercial actual.
        // Filtrar aqui tambien protege la vista aunque la BD conserve planes historicos activos.
        return planes.findByEstado("activo").stream()
                .filter(p -> "Plan Básico".equalsIgnoreCase(p.getNombrePlan())
                        || "Plan Profesional".equalsIgnoreCase(p.getNombrePlan()))
                .sorted(java.util.Comparator.comparing(PlanSuscripcion::getNombrePlan))
                .toList();
    }

    @Override
    public List<Pago> pagosEmpresa(Integer idEmpresa) {
        return pagos.findByEmpresa_IdEmpresaOrderByIdPagoDesc(idEmpresa);
    }

    @Override
    public List<Suscripcion> suscripcionesEmpresa(Integer idEmpresa) {
        return suscripciones.findByEmpresa_IdEmpresaOrderByIdSuscripcionDesc(idEmpresa);
    }

    @Override
    public List<Pago> pagosPendientes() {
        return pagos.findByEstadoOrderByIdPagoDesc(Pago.EstadoPago.PENDIENTE);
    }

    /**
     * Crea una suscripción pendiente y su solicitud de pago.
     *
     * @throws RecursoNoEncontradoException si el plan no existe.
     */
    @Override
    @Transactional
    public Pago checkout(Empresa empresa, Integer idPlan, String metodo,
                         String referencia) {
        PlanSuscripcion plan = planes.findById(idPlan)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un plan con id " + idPlan));
        Suscripcion suscripcion = new Suscripcion(empresa, plan);
        suscripciones.save(suscripcion);
        return pagos.save(new Pago(empresa, suscripcion, plan.getPrecio(),
                metodo, referencia));
    }

    /**
     * Verifica el pago y activa su suscripción, siempre que la empresa no
     * tenga otra suscripción activa diferente.
     */
    @Override
    @Transactional
    public void verificar(Integer id) {
        verificar(id, null);
    }

    @Override
    @Transactional
    public void verificar(Integer id, Integer idUsuarioVerifico) {
        Pago pago = pagos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un pago con id " + id));
        Suscripcion suscripcion = pago.getSuscripcion();
        Integer idEmpresa = suscripcion.getEmpresa().getIdEmpresa();
        suscripciones.findFirstByEmpresa_IdEmpresaAndEstadoOrderByIdSuscripcionDesc(
                        idEmpresa, Suscripcion.EstadoSuscripcion.ACTIVA)
                .filter(activa -> !activa.getIdSuscripcion()
                        .equals(suscripcion.getIdSuscripcion()))
                .ifPresent(activa -> {
                    throw new ReglaDeNegocioException(
                            "La empresa ya tiene una suscripcion activa");
                });
        pago.verificar(idUsuarioVerifico);
        suscripcion.activar();
    }

    @Override
    @Transactional
    public void rechazar(Integer id) {
        Pago pago = pagos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un pago con id " + id));
        pago.rechazar();
    }

    @Override
    public BigDecimal totalPagos() {
        return pagos.findAll().stream()
                .map(Pago::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
