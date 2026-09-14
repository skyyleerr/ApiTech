package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.ProduccionMielRequestDTO;
import com.apitech.mk5.dto.response.ProduccionMielResponseDTO;
import com.apitech.mk5.service.ProduccionMielService;
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
@RequestMapping("/api/produccion-miel")
public class ProduccionMielController {
    private final ProduccionMielService produccionService;

    public ProduccionMielController(ProduccionMielService produccionService) {
        this.produccionService = produccionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin_Cliente')")
    public ResponseEntity<ProduccionMielResponseDTO> crear(
            @Valid @RequestBody ProduccionMielRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produccionService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProduccionMielResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(produccionService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<ProduccionMielResponseDTO>> listar(@RequestParam Integer idEmpresa) {
        return ResponseEntity.ok(produccionService.listarPorEmpresa(idEmpresa));
    }

    @GetMapping("/colmena/{idColmena}")
    public ResponseEntity<List<ProduccionMielResponseDTO>> listarPorColmena(@PathVariable Integer idColmena) {
        return ResponseEntity.ok(produccionService.listarPorColmena(idColmena));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin_Cliente')")
    public ResponseEntity<ProduccionMielResponseDTO> actualizar(@PathVariable Integer id,
                                                                 @Valid @RequestBody ProduccionMielRequestDTO dto) {
        return ResponseEntity.ok(produccionService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin_Cliente')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        produccionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
