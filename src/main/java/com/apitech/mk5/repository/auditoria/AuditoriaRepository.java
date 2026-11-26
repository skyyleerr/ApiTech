package com.apitech.mk5.repository.auditoria;

import com.apitech.mk5.entity.auditoria.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {
    List<Auditoria> findAllByOrderByFechaDesc();
    List<Auditoria> findByUsuario_IdUsuarioOrderByFechaDesc(Integer idUsuario);
    List<Auditoria> findByEntidadTipoAndIdEntidadOrderByFechaDesc(String entidadTipo, Long idEntidad);
}
