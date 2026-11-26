package com.apitech.mk5.repository.apiario;

import com.apitech.mk5.entity.apiario.Medicion;
import com.apitech.mk5.entity.apiario.TipoMedicion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MedicionRepository extends JpaRepository<Medicion, Long> {
    Page<Medicion> findByAsociacion_IdAsociacion(Integer idAsociacion, Pageable pageable);
    Page<Medicion> findByAsociacion_IdAsociacionOrderByFechaDesc(Integer idAsociacion, Pageable pageable);
    Page<Medicion> findByAsociacion_IdAsociacionAndTipoMedicionAndFechaBetween(Integer idAsociacion, TipoMedicion tipoMedicion, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    Optional<Medicion> findTopByAsociacion_IdAsociacionOrderByFechaDesc(Integer idAsociacion);

    @Query("select m from Medicion m join m.asociacion a " +
           "where a.idEmpresa = :empresa " +
           "and (:asociacion is null or a.idAsociacion = :asociacion) " +
           "and (:tipo is null or m.tipoMedicion = :tipo) " +
           "and (:desde is null or m.fecha >= :desde) " +
           "and (:hasta is null or m.fecha <= :hasta) " +
           "order by m.fecha desc")
    Page<Medicion> buscarReporte(@Param("empresa") Integer empresa,
                                 @Param("asociacion") Integer asociacion,
                                 @Param("tipo") TipoMedicion tipo,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("hasta") LocalDateTime hasta,
                                 Pageable pageable);

    @Query("select count(m) from Medicion m join m.asociacion a " +
           "where a.idEmpresa = :empresa and (:asociacion is null or a.idAsociacion = :asociacion) " +
           "and (:tipo is null or m.tipoMedicion = :tipo) and (:desde is null or m.fecha >= :desde) " +
           "and (:hasta is null or m.fecha <= :hasta)")
    long contarReporte(@Param("empresa") Integer empresa, @Param("asociacion") Integer asociacion,
                       @Param("tipo") TipoMedicion tipo, @Param("desde") LocalDateTime desde,
                       @Param("hasta") LocalDateTime hasta);
}
