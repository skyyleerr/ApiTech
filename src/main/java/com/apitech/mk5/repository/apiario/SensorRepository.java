package com.apitech.mk5.repository.apiario;

import com.apitech.mk5.entity.apiario.Sensor;
import com.apitech.mk5.entity.apiario.TipoMedicion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link Sensor}.
 */
public interface SensorRepository extends JpaRepository<Sensor, Integer> {

    /**
     * Busca un sensor por su codigo unico (por ejemplo, el codigo
     * fisico grabado en un dispositivo ESP32).
     */
    Optional<Sensor> findByCodigo(String codigo);

    /**
     * Indica si ya existe un sensor con ese codigo. Util para validar
     * unicidad antes de un INSERT.
     */
    boolean existsByCodigo(String codigo);

    /**
     * Lista todos los sensores de una empresa.
     *
     * @param idEmpresa id de la empresa.
     * @return sensores cuya {@code empresa.idEmpresa} coincide.
     */
    List<Sensor> findByEmpresa_IdEmpresa(Integer idEmpresa);

    /**
     * Lista los sensores de una empresa filtrados ademas por tipo de
     * medicion (por ejemplo, solo los sensores de tipo TEMPERATURA).
     *
     * @param idEmpresa id de la empresa.
     * @param tipo      tipo de medicion a filtrar.
     * @return sensores que cumplen ambos criterios.
     */
    List<Sensor> findByEmpresa_IdEmpresaAndTipo(Integer idEmpresa, TipoMedicion tipo);

}
