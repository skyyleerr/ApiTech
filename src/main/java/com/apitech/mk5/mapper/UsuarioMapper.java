package com.apitech.mk5.mapper;

import com.apitech.mk5.dto.request.UsuarioActualizacionRequestDTO;
import com.apitech.mk5.dto.request.UsuarioRequestDTO;
import com.apitech.mk5.dto.response.UsuarioResponseDTO;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.usuario.Rol;
import com.apitech.mk5.entity.usuario.Usuario;

/**
 * Conversion entre {@link Usuario} y sus DTOs.
 *
 * <p><b>Por que estos metodos reciben {@code Empresa}/{@code Rol} ya
 * resueltos, en vez de solo los ids:</b> el mapper es transformacion
 * pura, no debe consultar la base de datos. Buscar la {@code Empresa}
 * y el {@code Rol} por su id (y validar que existan) es
 * responsabilidad del {@code service} -- el mapper solo ensambla el
 * objeto final una vez que el service ya tiene todo lo que necesita.</p>
 *
 * <p><b>Advertencia sobre carga perezosa (LAZY):</b> {@link #toResponseDTO}
 * llama a {@code usuario.getEmpresa()} y {@code usuario.getRol()}, que
 * son relaciones {@code FetchType.LAZY}. Esto SOLO funciona si el
 * metodo se ejecuta mientras la sesion de Hibernate sigue abierta --
 * es decir, dentro de un metodo de {@code service} anotado con
 * {@code @Transactional}. Si se llamara a este mapper despues de que la
 * transaccion ya se cerro, fallaria con un
 * {@code LazyInitializationException}.</p>
 */
public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    /**
     * Crea un {@link Usuario} NUEVO. {@code passwordHash} debe llegar
     * YA convertido a hash (bcrypt) -- este mapper nunca hashea, esa es
     * responsabilidad del {@code service} (que es quien tiene acceso al
     * {@code PasswordEncoder}).
     */
    public static Usuario toEntity(UsuarioRequestDTO dto, Empresa empresa, Rol rol, String passwordHash) {
        return new Usuario(
                empresa,
                rol,
                dto.nombre(),
                dto.apellido(),
                dto.correo(),
                passwordHash,
                dto.estado()
        );
    }

    /**
     * Aplica los datos de actualizacion sobre un {@link Usuario} ya
     * existente. Deliberadamente NO toca {@code password} -- eso pasa
     * por {@code UsuarioService.cambiarPassword(...)}, no por aqui.
     */
    public static void aplicarCambios(Usuario entidad, UsuarioActualizacionRequestDTO dto,
                                       Empresa empresa, Rol rol) {
        entidad.setEmpresa(empresa);
        entidad.setRol(rol);
        entidad.setNombre(dto.nombre());
        entidad.setApellido(dto.apellido());
        entidad.setCorreo(dto.correo());
        entidad.setEstado(dto.estado());
    }

    public static UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getIdUsuario(),
                usuario.getEmpresa() != null ? EmpresaMapper.toResponseDTO(usuario.getEmpresa()) : null,
                RolMapper.toResponseDTO(usuario.getRol()),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getEstado(),
                usuario.getFechaRegistro(),
                usuario.getFechaActualizacion(),
                usuario.getUltimoLogin()
        );
    }
}
