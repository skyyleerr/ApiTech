package com.apitech.mk5.controller;

import com.apitech.mk5.dto.response.MonitoreoResponseDTO;
import com.apitech.mk5.service.MonitoreoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST de {@code Monitoreo}.
 *
 * <p>{@link #activar} recibe {@code idAsociacion} como parametro de
 * consulta, no como cuerpo JSON: activar un monitoreo no necesita mas
 * dato que "sobre cual asociacion" -- no hace falta un DTO completo
 * para una operacion de un solo valor.</p>
 */
@RestController
@RequestMapping("/api/monitoreo")
public class MonitoreoController {

    private final MonitoreoService monitoreoService;

    public MonitoreoController(MonitoreoService monitoreoService) {
        this.monitoreoService = monitoreoService;
    }

    /**
     * Activa el monitoreo de una asociacion. Puede fallar con 409 si la
     * asociacion, el sensor o la colmena no estan en condiciones (ver
     * {@link MonitoreoService#activar}).
     */
    @PostMapping
    public ResponseEntity<MonitoreoResponseDTO> activar(@RequestParam Integer idAsociacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(monitoreoService.activar(idAsociacion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitoreoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(monitoreoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<MonitoreoResponseDTO>> listar(@RequestParam Integer idAsociacion) {
        return ResponseEntity.ok(monitoreoService.listarPorAsociacion(idAsociacion));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<MonitoreoResponseDTO> desactivar(@PathVariable Integer id) {
        return ResponseEntity.ok(monitoreoService.desactivar(id));
    }
}
