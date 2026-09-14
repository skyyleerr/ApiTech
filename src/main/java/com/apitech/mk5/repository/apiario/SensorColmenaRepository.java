package com.apitech.mk5.repository.apiario;

import com.apitech.mk5.entity.apiario.SensorColmena;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link SensorColmena}.
 */
public interface SensorColmenaRepository extends JpaRepository<SensorColmena, Integer> {

    /**
     * Busca la asociacion ACTIVA actual de un sensor (a lo sumo una,
     * por la regla de negocio de exclusividad). Se usara en el
     * {@code service} antes de crear una asociacion nueva, para
     * verificar que el sensor no este ya asociado a otra colmena.
     *
     * @param idSensor id del sensor.
     * @return la asociacion activa, o {@link Optional#empty()} si el
     *         sensor no tiene ninguna asociacion vigente.
     */
    Optional<SensorColmena> findBySensor_IdSensorAndActivoTrue(Integer idSensor);

    /**
     * Indica si un sensor ya tiene una asociacion activa. Mas eficiente
     * que traer la entidad completa cuando solo se necesita la
     * respuesta booleana.
     */
    boolean existsBySensor_IdSensorAndActivoTrue(Integer idSensor);

    List<SensorColmena> findByActivoTrue();

    List<SensorColmena> findByColmena_IdColmenaAndActivoTrue(Integer idColmena);

    /**
     * Historial completo de asociaciones de una colmena (activas e
     * inactivas), ordenado por defecto segun la base de datos.
     *
     * @param idColmena id de la colmena.
     */
    List<SensorColmena> findByColmena_IdColmena(Integer idColmena);

    /**
     * Historial completo de asociaciones de un sensor.
     *
     * @param idSensor id del sensor.
     */
    List<SensorColmena> findBySensor_IdSensor(Integer idSensor);

}
