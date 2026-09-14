package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.SensorColmenaRequestDTO;
import com.apitech.mk5.dto.response.SensorColmenaResponseDTO;
import com.apitech.mk5.exception.ValidacionException;
import com.apitech.mk5.service.SensorColmenaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST de {@code SensorColmena} (la asociacion historica
 * sensor-colmena).
 *
 * <p>{@link #listar} exige exactamente uno de los dos parametros
 * ({@code idColmena} o {@code idSensor}) -- son dos formas distintas de
 * ver el mismo historial (por colmena o por sensor), y pedir ambos o
 * ninguno no tiene un significado claro. La validacion de "cual de los
 * dos vino" es enrutamiento de la peticion, no logica de negocio, por
 * eso vive aqui y no en el service.</p>
 */
@RestController
@RequestMapping("/api/sensor-colmena")
public class SensorColmenaController {

    private final SensorColmenaService sensorColmenaService;

    public SensorColmenaController(SensorColmenaService sensorColmenaService) {
        this.sensorColmenaService = sensorColmenaService;
    }

    @PostMapping
    public ResponseEntity<SensorColmenaResponseDTO> crear(@Valid @RequestBody SensorColmenaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sensorColmenaService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SensorColmenaResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(sensorColmenaService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<SensorColmenaResponseDTO>> listar(
            @RequestParam(required = false) Integer idColmena,
            @RequestParam(required = false) Integer idSensor) {
        if (idColmena != null) {
            return ResponseEntity.ok(sensorColmenaService.listarPorColmena(idColmena));
        }
        if (idSensor != null) {
            return ResponseEntity.ok(sensorColmenaService.listarPorSensor(idSensor));
        }
        throw new ValidacionException("Debe indicar idColmena o idSensor para listar asociaciones");
    }

    /**
     * Desactiva la asociacion. Puede fallar con 409 si tiene un
     * monitoreo activo (ver {@link SensorColmenaService#desactivar}).
     */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<SensorColmenaResponseDTO> desactivar(@PathVariable Integer id) {
        return ResponseEntity.ok(sensorColmenaService.desactivar(id));
    }
}
