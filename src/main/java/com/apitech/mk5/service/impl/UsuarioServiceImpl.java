package com.apitech.mk5.service.impl;

import com.apitech.mk5.dto.request.CambioPasswordRequestDTO;
import com.apitech.mk5.dto.request.UsuarioActualizacionRequestDTO;
import com.apitech.mk5.dto.request.UsuarioRequestDTO;
import com.apitech.mk5.dto.request.PerfilUsuarioRequestDTO;
import com.apitech.mk5.dto.response.UsuarioResponseDTO;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.usuario.Rol;
import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.exception.RecursoNoEncontradoException;
import com.apitech.mk5.mapper.UsuarioMapper;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.repository.usuario.RolRepository;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import com.apitech.mk5.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Implementacion de {@link UsuarioService}.
 *
 * <p>El metodo privado {@link #validarCoherenciaRolEmpresa} es, en
 * terminos concretos, la replica en Java de los triggers
 * {@code trg_usuarios_empresa_ins} y {@code trg_usuarios_empresa_upd}
 * del esquema SQL -- la primera regla de negocio "de las que ya
 * veniamos anunciando como pendientes" desde la Etapa 2, ahora
 * implementada de verdad.</p>
 */
@Service
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    /** Nombres de rol que NUNCA deben tener empresa asignada. */
    private static final Set<String> ROLES_APITECH = Set.of("Admin_ApiTech", "Empleado_ApiTech");

    /** Nombres de rol que SIEMPRE deben tener empresa asignada. */
    private static final Set<String> ROLES_CLIENTE = Set.of("Admin_Cliente", "Empleado_Cliente");

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                               EmpresaRepository empresaRepository,
                               RolRepository rolRepository,
                               PasswordEncoder passwordEncoder,
                               com.apitech.mk5.service.EmpresaOperacionService empresaOperacion) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.empresaOperacion = empresaOperacion;
    }

    @Override
    @Transactional
    public UsuarioResponseDTO crear(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByCorreo(dto.correo())) {
            throw new ReglaDeNegocioException(
                    "Ya existe un usuario registrado con el correo " + dto.correo());
        }

        Rol rol = buscarRolOFallar(dto.idRol());
        Empresa empresa = dto.idEmpresa() != null ? buscarEmpresaOFallar(dto.idEmpresa()) : null;
        validarCoherenciaRolEmpresa(rol, empresa);
        if (empresa != null && ROLES_CLIENTE.contains(rol.getNombreRol())) empresaOperacion.validarPuedeOperar(empresa.getIdEmpresa());

        String passwordHash = passwordEncoder.encode(dto.password());
        Usuario usuario = UsuarioMapper.toEntity(dto, empresa, rol, passwordHash);
        usuarioRepository.save(usuario);

        return UsuarioMapper.toResponseDTO(usuario);
    }

    @Override
    public UsuarioResponseDTO obtenerPorId(Integer id) {
        return UsuarioMapper.toResponseDTO(buscarUsuarioOFallar(id));
    }

    @Override
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioMapper::toResponseDTO)
                .toList();
    }

    @Override
    public List<UsuarioResponseDTO> listarPorEmpresa(Integer idEmpresa) {
        return usuarioRepository.findByEmpresa_IdEmpresa(idEmpresa).stream()
                .map(UsuarioMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public UsuarioResponseDTO actualizar(Integer id, UsuarioActualizacionRequestDTO dto) {
        Usuario usuario = buscarUsuarioOFallar(id);
        if (usuario.getEmpresa() != null) empresaOperacion.validarPuedeOperar(usuario.getEmpresa().getIdEmpresa());

        boolean correoCambio = !usuario.getCorreo().equals(dto.correo());
        if (correoCambio && usuarioRepository.existsByCorreo(dto.correo())) {
            throw new ReglaDeNegocioException(
                    "Ya existe otro usuario registrado con el correo " + dto.correo());
        }

        Rol rol = buscarRolOFallar(dto.idRol());
        Empresa empresa = dto.idEmpresa() != null ? buscarEmpresaOFallar(dto.idEmpresa()) : null;
        validarCoherenciaRolEmpresa(rol, empresa);
        if (empresa != null && ROLES_CLIENTE.contains(rol.getNombreRol())) empresaOperacion.validarPuedeOperar(empresa.getIdEmpresa());

        UsuarioMapper.aplicarCambios(usuario, dto, empresa, rol);
        return UsuarioMapper.toResponseDTO(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO actualizarPerfil(Integer id, PerfilUsuarioRequestDTO dto) {
        Usuario usuario = buscarUsuarioOFallar(id);
        if (usuario.getEmpresa() != null) empresaOperacion.validarPuedeOperar(usuario.getEmpresa().getIdEmpresa());

        boolean correoCambio = !usuario.getCorreo().equalsIgnoreCase(dto.correo());
        if (correoCambio && usuarioRepository.existsByCorreo(dto.correo())) {
            throw new ReglaDeNegocioException(
                    "Ya existe otro usuario registrado con el correo " + dto.correo());
        }

        usuario.setNombre(dto.nombre().trim());
        usuario.setApellido(dto.apellido() == null ? null : dto.apellido().trim());
        usuario.setCorreo(dto.correo().trim());

        if (dto.passwordNuevo() != null && !dto.passwordNuevo().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.passwordNuevo()));
        }

        return UsuarioMapper.toResponseDTO(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO cambiarPassword(Integer id, CambioPasswordRequestDTO dto) {
        Usuario usuario = buscarUsuarioOFallar(id);
        if (usuario.getEmpresa() != null) empresaOperacion.validarPuedeOperar(usuario.getEmpresa().getIdEmpresa());
        usuario.setPassword(passwordEncoder.encode(dto.passwordNuevo()));
        return UsuarioMapper.toResponseDTO(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO cambiarEstado(Integer id, String nuevoEstado) {
        Usuario usuario = buscarUsuarioOFallar(id);
        if (usuario.getEmpresa() != null) empresaOperacion.validarPuedeOperar(usuario.getEmpresa().getIdEmpresa());
        usuario.setEstado(nuevoEstado);
        return UsuarioMapper.toResponseDTO(usuario);
    }

    @Override
    @Transactional
    public boolean eliminar(Integer id) {
        Usuario usuario = buscarUsuarioOFallar(id);
        if (usuario.getEmpresa() != null) empresaOperacion.validarPuedeOperar(usuario.getEmpresa().getIdEmpresa());
        try {
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // El usuario tiene datos dependientes (por ejemplo,
            // notificaciones o auditoría) protegidos con ON DELETE
            // RESTRICT/SET NULL: no se puede borrar la fila sin perder
            // esas referencias, así que se conserva el historial y se
            // degrada a una desactivación lógica.
            usuario.setEstado("inactivo");
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Helpers privados
    // ------------------------------------------------------------------

    /**
     * Replica {@code trg_usuarios_empresa_ins/upd}: valida que la
     * combinacion rol-empresa sea coherente ANTES de guardar. Si la
     * base de datos rechazara igual una combinacion invalida (el
     * trigger sigue activo), esta validacion adelantada evita que el
     * usuario final reciba un error generico de base de datos en vez
     * de un mensaje de negocio claro.
     */
    private void validarCoherenciaRolEmpresa(Rol rol, Empresa empresa) {
        String nombreRol = rol.getNombreRol();

        if (ROLES_APITECH.contains(nombreRol) && empresa != null) {
            throw new ReglaDeNegocioException(
                    "Los usuarios con rol " + nombreRol + " no deben tener una empresa asignada");
        }
        if (ROLES_CLIENTE.contains(nombreRol) && empresa == null) {
            throw new ReglaDeNegocioException(
                    "Los usuarios con rol " + nombreRol + " requieren una empresa asignada");
        }
    }

    private Usuario buscarUsuarioOFallar(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con id " + id));
    }

    private Rol buscarRolOFallar(Integer idRol) {
        return rolRepository.findById(idRol)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un rol con id " + idRol));
    }

    private Empresa buscarEmpresaOFallar(Integer idEmpresa) {
        return empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una empresa con id " + idEmpresa));
    }
}
