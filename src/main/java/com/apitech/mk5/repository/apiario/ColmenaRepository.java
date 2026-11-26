package com.apitech.mk5.repository.apiario;

import com.apitech.mk5.entity.apiario.Colmena;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link Colmena}.
 */
public interface ColmenaRepository extends JpaRepository<Colmena, Integer> {

    /**
     * Lista todas las colmenas de una empresa. Este es el criterio de
     * alcance correcto (por empresa) que debe usarse siempre -- nunca
     * un filtro por usuario individual, tal como se corrigio respecto
     * al comportamiento inconsistente detectado en MK3.5.
     *
     * @param idEmpresa id de la empresa.
     * @return colmenas cuya {@code empresa.idEmpresa} coincide.
     */
    List<Colmena> findByEmpresa_IdEmpresa(Integer idEmpresa);

    /**
     * Lista las colmenas de una empresa que ademas estan en un estado
     * especifico (por ejemplo, solo las "Estable").
     *
     * @param idEmpresa id de la empresa.
     * @param estado    estado exacto a filtrar.
     * @return colmenas que cumplen ambos criterios.
     */
    List<Colmena> findByEmpresa_IdEmpresaAndEstado(Integer idEmpresa, String estado);

}
