package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.CambioEstadoRequestDTO;
import com.apitech.mk5.dto.request.ColmenaRequestDTO;
import com.apitech.mk5.dto.response.ColmenaResponseDTO;
import com.apitech.mk5.service.ColmenaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Endpoints REST de {@code Colmena}.
 *
 * <p>{@code idEmpresa} es OBLIGATORIO en {@link #listar} (no tiene
 * {@code required = false}): a diferencia de {@code Usuario}, aqui no
 * existe un caso de uso legitimo de "listar todas las colmenas de todas
 * las empresas" -- el alcance por empresa es la unica forma correcta de
 * consultar colmenas, tal como se corrigio respecto al comportamiento
 * de MK3.5.</p>
 */
@RestController
@RequestMapping("/api/colmenas")
public class ColmenaController {

    private final ColmenaService colmenaService;

    public ColmenaController(ColmenaService colmenaService) {
        this.colmenaService = colmenaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin_Cliente')")
    public ResponseEntity<ColmenaResponseDTO> crear(@Valid @RequestBody ColmenaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(colmenaService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ColmenaResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(colmenaService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<ColmenaResponseDTO>> listar(@RequestParam Integer idEmpresa) {
        return ResponseEntity.ok(colmenaService.listarPorEmpresa(idEmpresa));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin_Cliente')")
    public ResponseEntity<ColmenaResponseDTO> actualizar(@PathVariable Integer id,
                                                          @Valid @RequestBody ColmenaRequestDTO dto) {
        return ResponseEntity.ok(colmenaService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('Admin_Cliente')")
    public ResponseEntity<ColmenaResponseDTO> cambiarEstado(@PathVariable Integer id,
                                                             @Valid @RequestBody CambioEstadoRequestDTO dto) {
        return ResponseEntity.ok(colmenaService.cambiarEstado(id, dto.nuevoEstado()));
    }
}
