package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.CambioPasswordRequestDTO;
import com.apitech.mk5.dto.request.UsuarioActualizacionRequestDTO;
import com.apitech.mk5.dto.request.UsuarioRequestDTO;
import com.apitech.mk5.dto.request.PerfilUsuarioRequestDTO;
import com.apitech.mk5.dto.response.UsuarioResponseDTO;

import java.util.List;

/**
 * Logica de negocio de {@code Usuario}.
 *
 * <p>Todos los metodos que reciben o modifican el {@code idRol}/{@code
 * idEmpresa} de un usuario aplican la regla de coherencia rol-empresa
 * (equivalente Java de {@code trg_usuarios_empresa_ins/upd}): un
 * usuario ApiTech (Admin_ApiTech/Empleado_ApiTech) nunca debe tener
 * empresa asignada; un usuario cliente (Admin_Cliente/Empleado_Cliente)
 * siempre debe tenerla.</p>
 */
public interface UsuarioService {

    /**
     * @throws com.apitech.mk5.exception.ReglaDeNegocioException si el
     *         correo ya esta en uso, o si la combinacion rol/empresa
     *         viola la regla de coherencia.
     * @throws com.apitech.mk5.exception.RecursoNoEncontradoException
     *         si el {@code idRol} o el {@code idEmpresa} indicados no existen.
     */
    UsuarioResponseDTO crear(UsuarioRequestDTO dto);

    UsuarioResponseDTO obtenerPorId(Integer id);

    /** Lista TODOS los usuarios del sistema (uso tipico: personal ApiTech). */
    List<UsuarioResponseDTO> listarTodos();

    /** Lista los usuarios de una empresa especifica (alcance correcto para un Admin_Cliente). */
    List<UsuarioResponseDTO> listarPorEmpresa(Integer idEmpresa);

    UsuarioResponseDTO actualizar(Integer id, UsuarioActualizacionRequestDTO dto);

    UsuarioResponseDTO cambiarPassword(Integer id, CambioPasswordRequestDTO dto);

    UsuarioResponseDTO actualizarPerfil(Integer id, PerfilUsuarioRequestDTO dto);

    UsuarioResponseDTO cambiarEstado(Integer id, String nuevoEstado);

    /**
     * Elimina definitivamente un usuario interno de ApiTech si no tiene
     * datos dependientes que lo impidan (por ejemplo, notificaciones);
     * en ese caso, degrada automáticamente a una desactivación lógica
     * (estado = "inactivo") para no perder el historial y así no romper
     * ninguna clave foránea existente.
     *
     * @return {@code true} si se eliminó definitivamente, {@code false}
     *         si en su lugar se desactivó porque tenía datos asociados.
     */
    boolean eliminar(Integer id);

}
