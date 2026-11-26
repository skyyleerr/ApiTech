package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.AuditoriaRequestDTO;
import com.apitech.mk5.dto.response.AuditoriaResponseDTO;
import com.apitech.mk5.service.AuditoriaService;
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
@RequestMapping("/api/auditoria")
@PreAuthorize("hasAnyRole('Admin_ApiTech','Empleado_ApiTech')")
public class AuditoriaController {
    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    public ResponseEntity<AuditoriaResponseDTO> crear(@Valid @RequestBody AuditoriaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditoriaService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(auditoriaService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<AuditoriaResponseDTO>> listar() {
        return ResponseEntity.ok(auditoriaService.listar());
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<AuditoriaResponseDTO>> listarPorUsuario(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(auditoriaService.listarPorUsuario(idUsuario));
    }

    @GetMapping("/entidad")
    public ResponseEntity<List<AuditoriaResponseDTO>> listarPorEntidad(
            @RequestParam String entidadTipo, @RequestParam Long idEntidad) {
        return ResponseEntity.ok(auditoriaService.listarPorEntidad(entidadTipo, idEntidad));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuditoriaResponseDTO> actualizar(@PathVariable Integer id,
                                                            @Valid @RequestBody AuditoriaRequestDTO dto) {
        return ResponseEntity.ok(auditoriaService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin_ApiTech')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        auditoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
