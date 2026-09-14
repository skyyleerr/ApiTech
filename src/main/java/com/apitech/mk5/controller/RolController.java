package com.apitech.mk5.controller;

import com.apitech.mk5.dto.response.RolResponseDTO;
import com.apitech.mk5.service.RolService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints REST de {@code Rol} -- solo lectura (ver la nota en
 * {@link com.apitech.mk5.dto.response.RolResponseDTO} sobre por que no
 * hay creacion/edicion expuesta).
 */
@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    public ResponseEntity<List<RolResponseDTO>> listarTodos() {
        return ResponseEntity.ok(rolService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(rolService.obtenerPorId(id));
    }
}
