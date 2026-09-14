package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.request.NotificacionRequestDTO;
import com.apitech.mk5.dto.response.NotificacionResponseDTO;
import com.apitech.mk5.entity.apiario.Alerta;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.evento.Notificacion;
import com.apitech.mk5.entity.suscripcion.Pago;
import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.mapper.NotificacionMapper;
import com.apitech.mk5.repository.apiario.AlertaRepository;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.repository.evento.NotificacionRepository;
import com.apitech.mk5.repository.suscripcion.PagoRepository;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import com.apitech.mk5.service.NotificacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificacionServiceImpl implements NotificacionService {
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final AlertaRepository alertaRepository;
    private final PagoRepository pagoRepository;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;

    public NotificacionServiceImpl(NotificacionRepository notificacionRepository,
                                   UsuarioRepository usuarioRepository,
                                   EmpresaRepository empresaRepository,
                                   AlertaRepository alertaRepository,
                                   PagoRepository pagoRepository,
                                   com.apitech.mk5.service.EmpresaOperacionService empresaOperacion) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.alertaRepository = alertaRepository;
        this.pagoRepository = pagoRepository;
        this.empresaOperacion = empresaOperacion;
    }

    @Override
    @Transactional
    public NotificacionResponseDTO crear(NotificacionRequestDTO dto) {
        Usuario usuario = buscarUsuario(dto.idUsuario());
        Alerta alerta = dto.idAlerta() == null ? null : buscarAlerta(dto.idAlerta());
        Pago pago = dto.idPago() == null ? null : buscarPago(dto.idPago());
        Empresa empresa = derivarEmpresa(usuario, alerta, pago);
        if (empresa != null) empresaOperacion.validarPuedeOperar(empresa.getIdEmpresa());
        Notificacion entidad = NotificacionMapper.toEntity(dto, usuario, empresa, alerta, pago);
        return NotificacionMapper.toResponseDTO(notificacionRepository.save(entidad));
    }

    @Override
    public NotificacionResponseDTO obtenerPorId(Integer id) {
        return NotificacionMapper.toResponseDTO(buscarNotificacion(id));
    }

    @Override
    public List<NotificacionResponseDTO> listar(Integer idEmpresa) {
        List<Notificacion> notificaciones = idEmpresa == null
                ? notificacionRepository.findAllByOrderByFechaDesc()
                : notificacionRepository.findByEmpresa_IdEmpresaOrderByFechaDesc(idEmpresa);
        return notificaciones.stream().map(NotificacionMapper::toResponseDTO).toList();
    }

    @Override
    public List<NotificacionResponseDTO> listarPorUsuario(Integer idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new RecursoNoEncontradoException("No existe un usuario con id " + idUsuario);
        }
        return notificacionRepository.findByUsuario_IdUsuarioOrderByFechaDesc(idUsuario)
                .stream().map(NotificacionMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional
    public NotificacionResponseDTO actualizar(Integer id, NotificacionRequestDTO dto) {
        Notificacion entidad = buscarNotificacion(id);
        Usuario usuario = buscarUsuario(dto.idUsuario());
        Alerta alerta = dto.idAlerta() == null ? null : buscarAlerta(dto.idAlerta());
        Pago pago = dto.idPago() == null ? null : buscarPago(dto.idPago());
        Empresa empresa = derivarEmpresa(usuario, alerta, pago);
        if (empresa != null) empresaOperacion.validarPuedeOperar(empresa.getIdEmpresa());
        NotificacionMapper.aplicarCambios(entidad, dto, usuario, empresa, alerta, pago);
        return NotificacionMapper.toResponseDTO(entidad);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Notificacion n = buscarNotificacion(id);
        if (n.getEmpresa() != null) empresaOperacion.validarPuedeOperar(n.getEmpresa().getIdEmpresa());
        notificacionRepository.delete(n);
    }

    private Empresa derivarEmpresa(Usuario usuario, Alerta alerta, Pago pago) {
        Integer empresaUsuario = usuario.getEmpresa() == null ? null : usuario.getEmpresa().getIdEmpresa();
        Integer empresaAlerta = alerta == null ? null : alerta.getIdEmpresa();
        Integer empresaPago = pago == null || pago.getEmpresa() == null
                ? null : pago.getEmpresa().getIdEmpresa();
        if (empresaAlerta != null && empresaPago != null && !empresaAlerta.equals(empresaPago)) {
            throw new ReglaDeNegocioException("La alerta y el pago deben pertenecer a la misma empresa");
        }
        Integer empresaReferencia = empresaAlerta != null ? empresaAlerta : empresaPago;
        if (empresaUsuario != null && empresaReferencia != null
                && !empresaUsuario.equals(empresaReferencia)) {
            throw new ReglaDeNegocioException("El usuario y las referencias deben pertenecer a la misma empresa");
        }
        Integer idEmpresa = empresaUsuario != null ? empresaUsuario : empresaReferencia;
        return idEmpresa == null ? null : empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una empresa con id " + idEmpresa));
    }

    private Notificacion buscarNotificacion(Integer id) {
        return notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una notificacion con id " + id));
    }

    private Usuario buscarUsuario(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con id " + id));
    }

    private Alerta buscarAlerta(Integer id) {
        return alertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una alerta con id " + id));
    }

    private Pago buscarPago(Integer id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un pago con id " + id));
    }
}
