package com.apitech.mk5.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.repository.usuario.UsuarioRepository;

/**
 * Componente auxiliar para obtener el {@link Usuario} autenticado
 * actualmente desde cualquier parte de la aplicación.
 *
 * <p>Centraliza el acceso a {@code SecurityContextHolder} para que
 * los servicios y controladores no dependan directamente de Spring
 * Security al necesitar datos del usuario en sesión.</p>
 *
 * <h3>Ejemplo de uso en un service:</h3>
 * <pre>{@code
 * Usuario actual = usuarioContexto.getUsuarioActual();
 * Integer idEmpresa = actual.getEmpresa().getIdEmpresa();
 * }</pre>
 */
@Component
@Transactional(readOnly = true)
public class UsuarioContexto {

    private final UsuarioRepository usuarioRepository;

    public UsuarioContexto(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Devuelve el {@link Usuario} correspondiente al principal
     * autenticado en la sesión actual.
     *
     * @return Usuario autenticado.
     * @throws IllegalStateException si no hay sesión activa.
     */
    public Usuario getUsuarioActual() {
        String correo = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró el usuario autenticado: " + correo));
    }

    /**
     * Devuelve el id de la empresa del usuario autenticado.
     * {@code null} para usuarios internos de ApiTech.
     */
    public Integer getIdEmpresaActual() {
        Usuario u = getUsuarioActual();
        return u.getEmpresa() != null ? u.getEmpresa().getIdEmpresa() : null;
    }

    /**
     * Verifica si el usuario autenticado tiene un rol específico.
     *
     * @param nombreRol nombre exacto del rol (sin prefijo ROLE_).
     */
    public boolean tieneRol(String nombreRol) {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + nombreRol));
    }
}