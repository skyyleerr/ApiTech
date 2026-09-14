package com.apitech.mk5.repository.apiario;

import com.apitech.mk5.entity.apiario.Monitoreo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link Monitoreo}.
 */
public interface MonitoreoRepository extends JpaRepository<Monitoreo, Integer> {

    /**
     * Busca el monitoreo con un estado especifico para una asociacion
     * dada (tipicamente se usara con {@code estado = "activo"} para
     * encontrar el monitoreo vigente de una asociacion, si existe).
     *
     * @param idAsociacion id de la asociacion sensor-colmena.
     * @param estado       estado exacto a buscar.
     * @return el monitoreo encontrado, o {@link Optional#empty()}.
     */
    Optional<Monitoreo> findByAsociacion_IdAsociacionAndEstado(Integer idAsociacion, String estado);

    /**
     * Indica si una asociacion ya tiene un monitoreo en un estado dado.
     * Util para el {@code service} antes de intentar activar uno nuevo.
     */
    boolean existsByAsociacion_IdAsociacionAndEstado(Integer idAsociacion, String estado);

    /**
     * Historial completo de monitoreos de una asociacion sensor-colmena.
     *
     * @param idAsociacion id de la asociacion.
     */
    List<Monitoreo> findByAsociacion_IdAsociacion(Integer idAsociacion);

}
