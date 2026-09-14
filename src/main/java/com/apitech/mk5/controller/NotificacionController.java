package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.NotificacionRequestDTO;
import com.apitech.mk5.dto.response.NotificacionResponseDTO;
import com.apitech.mk5.service.NotificacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {
    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin_ApiTech','Empleado_ApiTech','Admin_Cliente')")
    public ResponseEntity<NotificacionResponseDTO> crear(@Valid @RequestBody NotificacionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificacionService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(notificacionService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponseDTO>> listar(
            @RequestParam(required = false) Integer idEmpresa) {
        return ResponseEntity.ok(notificacionService.listar(idEmpresa));
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<NotificacionResponseDTO>> listarPorUsuario(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(notificacionService.listarPorUsuario(idUsuario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin_ApiTech','Empleado_ApiTech','Admin_Cliente','Empleado_Cliente')")
    public ResponseEntity<NotificacionResponseDTO> actualizar(@PathVariable Integer id,
                                                               @Valid @RequestBody NotificacionRequestDTO dto) {
        return ResponseEntity.ok(notificacionService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin_ApiTech','Admin_Cliente')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        notificacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
