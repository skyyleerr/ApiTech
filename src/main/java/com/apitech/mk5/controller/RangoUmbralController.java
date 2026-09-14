package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.RangoUmbralRequestDTO;
import com.apitech.mk5.dto.response.RangoUmbralResponseDTO;
import com.apitech.mk5.entity.apiario.TipoMedicion;
import com.apitech.mk5.service.RangoUmbralService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/umbrales")
public class RangoUmbralController {

    private final RangoUmbralService rangoUmbralService;

    public RangoUmbralController(RangoUmbralService rangoUmbralService) {
        this.rangoUmbralService = rangoUmbralService;
    }

    @PostMapping
    public ResponseEntity<RangoUmbralResponseDTO> crear(@Valid @RequestBody RangoUmbralRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rangoUmbralService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RangoUmbralResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(rangoUmbralService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<RangoUmbralResponseDTO>> listar(@RequestParam Integer idEmpresa) {
        return ResponseEntity.ok(rangoUmbralService.listarPorEmpresa(idEmpresa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RangoUmbralResponseDTO> actualizar(@PathVariable Integer id,
                                                              @Valid @RequestBody RangoUmbralRequestDTO dto) {
        return ResponseEntity.ok(rangoUmbralService.actualizar(id, dto));
    }

    /**
     * Resuelve el umbral aplicable a una empresa y tipo de medicion,
     * aplicando la prioridad especifico-luego-global (ver
     * {@link RangoUmbralService#resolverUmbralAplicable}). Spring
     * convierte automaticamente el texto del query param
     * (por ejemplo "TEMPERATURA") al enum {@link TipoMedicion}.
     */
    @GetMapping("/resolver")
    public ResponseEntity<RangoUmbralResponseDTO> resolver(@RequestParam Integer idEmpresa,
                                                            @RequestParam TipoMedicion tipoMedicion) {
        return ResponseEntity.ok(rangoUmbralService.resolverUmbralAplicable(idEmpresa, tipoMedicion));
    }
}
