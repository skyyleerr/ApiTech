package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.request.NotificacionRequestDTO;
import com.apitech.mk5.dto.response.NotificacionResponseDTO;
import com.apitech.mk5.entity.apiario.Alerta;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.evento.Notificacion;
import com.apitech.mk5.entity.suscripcion.Pago;
import com.apitech.mk5.entity.usuario.Usuario;

public final class NotificacionMapper {
    private NotificacionMapper() {
    }

    public static Notificacion toEntity(NotificacionRequestDTO dto, Usuario usuario,
                                        Empresa empresa, Alerta alerta, Pago pago) {
        return new Notificacion(usuario, empresa, dto.tipo(), dto.titulo(), dto.mensaje(),
                dto.leida(), alerta, pago);
    }

    public static void aplicarCambios(Notificacion entidad, NotificacionRequestDTO dto,
                                      Usuario usuario, Empresa empresa, Alerta alerta, Pago pago) {
        entidad.setUsuario(usuario);
        entidad.setEmpresa(empresa);
        entidad.setTipo(dto.tipo());
        entidad.setTitulo(dto.titulo());
        entidad.setMensaje(dto.mensaje());
        entidad.setLeida(dto.leida());
        entidad.setAlerta(alerta);
        entidad.setPago(pago);
    }

    public static NotificacionResponseDTO toResponseDTO(Notificacion entidad) {
        return new NotificacionResponseDTO(
                entidad.getIdNotificacion(),
                entidad.getUsuario() != null ? entidad.getUsuario().getIdUsuario() : null,
                entidad.getEmpresa() != null ? entidad.getEmpresa().getIdEmpresa() : null,
                entidad.getTipo(), entidad.getTitulo(), entidad.getMensaje(), entidad.isLeida(),
                entidad.getFecha(),
                entidad.getAlerta() != null ? entidad.getAlerta().getIdAlerta() : null,
                entidad.getPago() != null ? entidad.getPago().getIdPago() : null
        );
    }
}
