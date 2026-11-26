package com.apitech.mk5.repository.evento;

import com.apitech.mk5.entity.evento.ProduccionMiel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ProduccionMielRepository extends JpaRepository<ProduccionMiel, Integer> {
    List<ProduccionMiel> findByEmpresa_IdEmpresaOrderByFechaDesc(Integer idEmpresa);
    List<ProduccionMiel> findByColmena_IdColmenaOrderByFechaDesc(Integer idColmena);
    List<ProduccionMiel> findByEmpresa_IdEmpresaAndFechaBetweenOrderByFechaDesc(Integer idEmpresa, LocalDate desde, LocalDate hasta);
}
