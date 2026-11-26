package com.apitech.mk5.repository.suscripcion;

import com.apitech.mk5.entity.suscripcion.SolicitudSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SolicitudSuscripcionRepository extends JpaRepository<SolicitudSuscripcion, Integer> {
    List<SolicitudSuscripcion> findAllByOrderByIdSolicitudDesc();
    List<SolicitudSuscripcion> findByEstadoOrderByIdSolicitudDesc(SolicitudSuscripcion.EstadoSolicitud estado);
    boolean existsByNitAndEstado(String nit, SolicitudSuscripcion.EstadoSolicitud estado);
    boolean existsByNitAndEstadoAndIdSolicitudNot(String nit, SolicitudSuscripcion.EstadoSolicitud estado, Integer idSolicitud);
}
