package com.apitech.mk5.repository.empresa;

import com.apitech.mk5.entity.empresa.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link Empresa}.
 *
 * <p>Al extender {@code JpaRepository<Empresa, Integer>} se obtienen de
 * forma automatica, sin escribir una sola linea de SQL ni de
 * implementacion, los metodos CRUD basicos: {@code save}, {@code findById},
 * {@code findAll}, {@code deleteById}, {@code existsById}, {@code count},
 * entre otros. Spring genera la implementacion real en tiempo de
 * ejecucion (un proxy dinamico).</p>
 *
 * <p>Los metodos adicionales declarados aqui abajo son "metodos de
 * consulta derivados": Spring Data JPA interpreta el nombre del metodo
 * (siguiendo una convencion) y construye la consulta JPQL/SQL
 * correspondiente automaticamente -- no requieren cuerpo ni
 * implementacion manual.</p>
 */
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {

    /**
     * Busca una empresa por su NIT.
     *
     * <p>Se usa {@link Optional} en vez de devolver {@code null}
     * directamente para forzar, a nivel de tipo, a que quien consuma
     * este metodo maneje explicitamente el caso "no existe" -- por
     * ejemplo, en el flujo de login por NIT que ya identificamos en el
     * analisis de MK3.5.</p>
     *
     * @param nit NIT exacto de la empresa a buscar.
     * @return la empresa encontrada, o {@link Optional#empty()} si no
     *         existe ninguna con ese NIT.
     */
    Optional<Empresa> findByNit(String nit);

    /**
     * Indica si ya existe una empresa registrada con el NIT dado.
     *
     * <p>Util para validaciones de unicidad en el service antes de
     * intentar un INSERT (evita depender unicamente de que la base de
     * datos rechace el duplicado por la restriccion UNIQUE).</p>
     *
     * @param nit NIT a verificar.
     * @return {@code true} si ya existe una empresa con ese NIT.
     */
    boolean existsByNit(String nit);

    java.util.List<Empresa> findByNombreEmpresaContainingIgnoreCaseOrderByNombreEmpresaAsc(String nombreEmpresa);

}
