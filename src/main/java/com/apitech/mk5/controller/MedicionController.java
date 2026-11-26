package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.MedicionRequestDTO;
import com.apitech.mk5.dto.response.MedicionResponseDTO;
import com.apitech.mk5.entity.apiario.TipoMedicion;
import com.apitech.mk5.service.MedicionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Endpoints REST de {@code Medicion}.
 *
 * <p>Los metodos de listado reciben {@link Pageable} directamente como
 * parametro -- Spring lo resuelve automaticamente a partir de los query
 * params {@code page}, {@code size} y {@code sort} (por ejemplo,
 * {@code ?page=0&size=20&sort=fecha,desc}). Es el reflejo, a nivel de
 * API, de la decision tomada en la Etapa 5: nunca exponer un listado de
 * mediciones sin paginar.</p>
 */
@RestController
@RequestMapping("/api/mediciones")
public class MedicionController {

    private final MedicionService medicionService;

    public MedicionController(MedicionService medicionService) {
        this.medicionService = medicionService;
    }

    /**
     * Registra una medicion. Puede fallar con 409 si la asociacion no
     * tiene monitoreo activo, o si el tipo/origen no coinciden con el
     * sensor (ver {@link MedicionService#registrar}).
     */
    @PostMapping
    public ResponseEntity<MedicionResponseDTO> registrar(@Valid @RequestBody MedicionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicionService.registrar(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(medicionService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<MedicionResponseDTO>> listar(@RequestParam Integer idAsociacion,
                                                              Pageable pageable) {
        return ResponseEntity.ok(medicionService.listarPorAsociacion(idAsociacion, pageable));
    }

    /**
     * Consulta multicriterio (asociacion + tipo + rango de fechas) --
     * el punto de integracion pensado para el futuro modulo de reportes.
     */
    @GetMapping("/rango")
    public ResponseEntity<Page<MedicionResponseDTO>> listarPorRango(
            @RequestParam Integer idAsociacion,
            @RequestParam TipoMedicion tipoMedicion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            Pageable pageable) {
        return ResponseEntity.ok(
                medicionService.listarPorRango(idAsociacion, tipoMedicion, desde, hasta, pageable));
    }

    @GetMapping("/ultima")
    public ResponseEntity<MedicionResponseDTO> obtenerUltima(@RequestParam Integer idAsociacion) {
        return ResponseEntity.ok(medicionService.obtenerUltima(idAsociacion));
    }
}
