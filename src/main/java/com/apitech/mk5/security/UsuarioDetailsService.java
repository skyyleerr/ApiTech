package com.apitech.mk5.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import jakarta.servlet.http.HttpSession;

/**
 * Implementación de {@link UserDetailsService} para Spring Security.
 *
 * <p>Spring Security llama a {@link #loadUserByUsername} durante el
 * proceso de autenticación para obtener los datos del usuario desde
 * la base de datos. El "username" en este sistema es el correo
 * electrónico.</p>
 *
 * <p>Los roles se convierten al formato que espera Spring Security:
 * el prefijo {@code ROLE_} es obligatorio para que funcionen las
 * anotaciones como {@code @PreAuthorize("hasRole('Admin_ApiTech')")}
 * o las expresiones en {@code SecurityConfig}.</p>
 */
@Service
@Transactional(readOnly = true)
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe un usuario con el correo: " + correo));

        if (!"activo".equals(usuario.getEstado())) {
            throw new UsernameNotFoundException(
                    "El usuario está " + usuario.getEstado());
        }

        String rolNombre = usuario.getRol().getNombreRol();
        if (rolNombre.equals("Admin_Cliente") || rolNombre.equals("Empleado_Cliente")) {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpSession session = attrs.getRequest().getSession(false);
                Integer empresaLogin = session == null ? null : (Integer) session.getAttribute("LOGIN_EMPRESA_ID");
                if (empresaLogin == null || usuario.getEmpresa() == null
                        || !empresaLogin.equals(usuario.getEmpresa().getIdEmpresa())) {
                    throw new UsernameNotFoundException("El usuario no pertenece a la empresa seleccionada");
                }
            }
        }

        // Convierte el rol a GrantedAuthority con prefijo ROLE_
        String authority = "ROLE_" + usuario.getRol().getNombreRol();

        return org.springframework.security.core.userdetails.User
                .withUsername(usuario.getCorreo())
                .password(usuario.getPassword())
                .authorities(new SimpleGrantedAuthority(authority))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}