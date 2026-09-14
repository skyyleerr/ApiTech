package com.apitech.mk5.repository.evento;

import com.apitech.mk5.entity.evento.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    List<Notificacion> findAllByOrderByFechaDesc();
    List<Notificacion> findByEmpresa_IdEmpresaOrderByFechaDesc(Integer idEmpresa);
    List<Notificacion> findByUsuario_IdUsuarioOrderByFechaDesc(Integer idUsuario);
}
