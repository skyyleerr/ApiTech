package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.CambioEstadoRequestDTO;
import com.apitech.mk5.dto.request.SensorRequestDTO;
import com.apitech.mk5.dto.response.SensorResponseDTO;
import com.apitech.mk5.service.SensorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensores")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @PostMapping
    public ResponseEntity<SensorResponseDTO> crear(@Valid @RequestBody SensorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sensorService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SensorResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(sensorService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<SensorResponseDTO>> listar(@RequestParam Integer idEmpresa) {
        return ResponseEntity.ok(sensorService.listarPorEmpresa(idEmpresa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SensorResponseDTO> actualizar(@PathVariable Integer id,
                                                         @Valid @RequestBody SensorRequestDTO dto) {
        return ResponseEntity.ok(sensorService.actualizar(id, dto));
    }

    /**
     * Cambia el estado del sensor. Puede fallar con 409 si se intenta
     * desactivar un sensor con monitoreo activo (ver {@link SensorService#cambiarEstado}).
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<SensorResponseDTO> cambiarEstado(@PathVariable Integer id,
                                                            @Valid @RequestBody CambioEstadoRequestDTO dto) {
        return ResponseEntity.ok(sensorService.cambiarEstado(id, dto.nuevoEstado()));
    }
}
