package com.apitech.mk5.repository.apiario;

import com.apitech.mk5.entity.apiario.RangoUmbral;
import com.apitech.mk5.entity.apiario.TipoMedicion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link RangoUmbral}.
 *
 * <p>Los dos metodos de aqui son las piezas que {@code RangoUmbralService}
 * combinara para implementar la regla de prioridad documentada en
 * {@link RangoUmbral} (primero buscar el especifico de la empresa, si
 * no existe usar el global). Este repository NO decide esa prioridad
 * -- eso es logica de negocio y le corresponde al service.</p>
 */
public interface RangoUmbralRepository extends JpaRepository<RangoUmbral, Integer> {

    /**
     * Busca el umbral especifico de una empresa para un tipo de
     * medicion dado.
     */
    Optional<RangoUmbral> findByEmpresa_IdEmpresaAndTipoMedicion(Integer idEmpresa, TipoMedicion tipoMedicion);

    /**
     * Busca el umbral GLOBAL (sin empresa asociada) para un tipo de
     * medicion dado.
     */
    Optional<RangoUmbral> findByEmpresaIsNullAndTipoMedicion(TipoMedicion tipoMedicion);

    /**
     * Lista todos los umbrales especificos de una empresa (no incluye
     * los globales). Se agrega en la Etapa 10 para soportar un
     * endpoint de administracion de umbrales por empresa.
     */
    java.util.List<RangoUmbral> findByEmpresa_IdEmpresa(Integer idEmpresa);

}
