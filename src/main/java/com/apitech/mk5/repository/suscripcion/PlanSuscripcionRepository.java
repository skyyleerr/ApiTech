package com.apitech.mk5.repository.suscripcion;
import com.apitech.mk5.entity.suscripcion.PlanSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PlanSuscripcionRepository extends JpaRepository<PlanSuscripcion,Integer>{ List<PlanSuscripcion> findByEstado(String estado); java.util.Optional<PlanSuscripcion> findByNombrePlan(String nombrePlan); }
