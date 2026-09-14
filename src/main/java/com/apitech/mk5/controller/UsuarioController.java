package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.CambioEstadoRequestDTO;
import com.apitech.mk5.dto.request.CambioPasswordRequestDTO;
import com.apitech.mk5.dto.request.UsuarioActualizacionRequestDTO;
import com.apitech.mk5.dto.request.UsuarioRequestDTO;
import com.apitech.mk5.dto.response.UsuarioResponseDTO;
import com.apitech.mk5.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST de {@code Usuario}.
 *
 * <p>{@link #listar} acepta un parametro opcional {@code idEmpresa}:
 * si se envia, delega en {@code UsuarioService.listarPorEmpresa(...)}
 * (el alcance correcto para un Admin_Cliente viendo solo su equipo);
 * si no se envia, delega en {@code listarTodos()} (uso tipico de
 * personal ApiTech viendo todo el sistema). La decision de CUAL usar
 * la toma este metodo con un simple condicional sobre un parametro de
 * la peticion -- eso no es logica de negocio, es enrutamiento de la
 * propia peticion HTTP, por eso sigue siendo aceptable aqui y no en el
 * service.</p>
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO creado = usuarioService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listar(
            @RequestParam(required = false) Integer idEmpresa) {
        List<UsuarioResponseDTO> resultado = idEmpresa != null
                ? usuarioService.listarPorEmpresa(idEmpresa)
                : usuarioService.listarTodos();
        return ResponseEntity.ok(resultado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizar(@PathVariable Integer id,
                                                          @Valid @RequestBody UsuarioActualizacionRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<UsuarioResponseDTO> cambiarPassword(@PathVariable Integer id,
                                                               @Valid @RequestBody CambioPasswordRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.cambiarPassword(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponseDTO> cambiarEstado(@PathVariable Integer id,
                                                             @Valid @RequestBody CambioEstadoRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, dto.nuevoEstado()));
    }
}
