package com.apitech.mk5.service;

import com.apitech.mk5.entity.suscripcion.SolicitudSuscripcion;
import java.math.BigDecimal;
import java.util.List;

public interface SolicitudSuscripcionService {
    SolicitudSuscripcion crear(String nombreEmpresa, String nit, String correoEmpresa,
                               String telefono, String direccion, String nombreContacto,
                               String correoContacto, String telefonoContacto, String tarjetaFalsa, Integer idPlan);
    List<SolicitudSuscripcion> listarTodas();
    List<SolicitudSuscripcion> listarPendientes();
    SolicitudSuscripcion obtener(Integer id);
    SolicitudSuscripcion actualizar(Integer id, String nombreEmpresa, String nit,
                                    String correoEmpresa, String telefono, String direccion,
                                    String nombreContacto, String correoContacto,
                                    String telefonoContacto, Integer idPlan);
    void rechazar(Integer id, Integer idUsuarioRevisor, String motivo);
    void aprobar(Integer id, Integer idUsuarioRevisor, String nombreEmpresa, String nit,
                 String correoEmpresa, String telefono, String direccion,
                 String correoAdmin, String passwordInicial, Integer idPlan,
                 String metodoPago, String referenciaPago);
}
