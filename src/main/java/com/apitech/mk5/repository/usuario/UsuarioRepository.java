package com.apitech.mk5.repository.usuario;

import com.apitech.mk5.entity.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link Usuario}.
 *
 * <p>Ademas de los metodos derivados simples (sobre columnas propias de
 * {@code usuarios}), incluye metodos que atraviesan relaciones hacia
 * otras entidades usando la notacion de guion bajo
 * ({@code Empresa_IdEmpresa}, {@code Rol_NombreRol}): Spring Data JPA
 * interpreta {@code Empresa_IdEmpresa} como "sigue la relacion
 * {@code empresa} de Usuario, y dentro de esa Empresa compara su campo
 * {@code idEmpresa}" -- equivalente a un JOIN generado automaticamente,
 * sin escribir SQL ni JPQL a mano.</p>
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /**
     * Busca un usuario por su correo (unico en todo el sistema, no solo
     * dentro de una empresa).
     */
    Optional<Usuario> findByCorreo(String correo);

    /**
     * Indica si ya existe un usuario registrado con ese correo. Util
     * para validar unicidad antes de un INSERT, en vez de depender solo
     * de que la base de datos rechace el duplicado.
     */
    boolean existsByCorreo(String correo);

    /**
     * Lista todos los usuarios de una empresa especifica (por su id).
     *
     * <p>Este es el metodo que refleja la correccion que identificamos
     * en el analisis de MK3.5: el alcance de "usuarios visibles" debe
     * resolverse siempre por empresa, nunca por un criterio distinto.</p>
     *
     * @param idEmpresa id de la empresa.
     * @return usuarios cuya {@code empresa.idEmpresa} coincide.
     */
    List<Usuario> findByEmpresa_IdEmpresa(Integer idEmpresa);

    /**
     * Lista todos los usuarios que tienen un rol determinado (por su
     * nombre exacto, por ejemplo "Admin_Cliente").
     *
     * @param nombreRol nombre del rol tal como esta en la tabla {@code roles}.
     * @return usuarios cuyo {@code rol.nombreRol} coincide.
     */
    List<Usuario> findByRol_NombreRol(String nombreRol);

    /**
     * Lista los usuarios cuyo rol coincide con alguno de los nombres
     * dados, trayendo el {@code rol} en la misma consulta (JOIN FETCH)
     * para que la vista pueda leer {@code usuario.rol.nombreRol} sin
     * arriesgar un problema de carga perezosa. Se usa para listar el
     * equipo interno de ApiTech (Admin_ApiTech / Empleado_ApiTech) en
     * el panel "Usuarios ApiTech".
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT u FROM Usuario u JOIN FETCH u.rol r WHERE r.nombreRol IN :nombresRol ORDER BY u.idUsuario ASC")
    List<Usuario> findByRolNombreInConRol(
            @org.springframework.data.repository.query.Param("nombresRol") java.util.Collection<String> nombresRol);

}
