package com.apitech.mk5.repository.apiario;

import com.apitech.mk5.entity.apiario.Alerta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Integer> {
    Page<Alerta> findByIdEmpresaAndEstado(Integer idEmpresa, String estado, Pageable pageable);
    Page<Alerta> findByIdEmpresaAndSeveridadAndEstado(Integer idEmpresa, String severidad, String estado, Pageable pageable);
    Page<Alerta> findByColmena_IdColmena(Integer idColmena, Pageable pageable);
    Optional<Alerta> findTopByColmena_IdColmenaOrderByFechaDesc(Integer idColmena);
    boolean existsByIdEmpresaAndColmena_IdColmenaAndTipoAndEstado(Integer idEmpresa, Integer idColmena, String tipo, String estado);
    long countByIdEmpresaAndEstado(Integer idEmpresa, String estado);
    long countByIdEmpresa(Integer idEmpresa);
    long countByIdEmpresaAndFechaBetween(Integer idEmpresa, LocalDateTime desde, LocalDateTime hasta);
}
