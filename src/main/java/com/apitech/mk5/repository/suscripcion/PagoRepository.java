package com.apitech.mk5.repository.suscripcion;
import com.apitech.mk5.entity.suscripcion.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PagoRepository extends JpaRepository<Pago,Integer>{ List<Pago> findByEmpresa_IdEmpresaOrderByIdPagoDesc(Integer idEmpresa); List<Pago> findByEstadoOrderByIdPagoDesc(Pago.EstadoPago estado); long countByEstado(Pago.EstadoPago estado); }
