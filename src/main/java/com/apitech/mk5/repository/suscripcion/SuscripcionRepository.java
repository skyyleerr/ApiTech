package com.apitech.mk5.repository.suscripcion;
import com.apitech.mk5.entity.suscripcion.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SuscripcionRepository extends JpaRepository<Suscripcion,Integer>{ List<Suscripcion> findByEmpresa_IdEmpresaOrderByIdSuscripcionDesc(Integer idEmpresa); Optional<Suscripcion> findFirstByEmpresa_IdEmpresaAndEstadoOrderByIdSuscripcionDesc(Integer idEmpresa, Suscripcion.EstadoSuscripcion estado);
    List<Suscripcion> findAllByOrderByIdSuscripcionDesc(); }
