package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.CambioEstadoRequestDTO;
import com.apitech.mk5.dto.request.EmpresaRequestDTO;
import com.apitech.mk5.dto.response.EmpresaResponseDTO;
import com.apitech.mk5.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST de {@code Empresa}.
 *
 * <p>Este controller es deliberadamente "tonto": cada metodo hace
 * exactamente tres cosas -- recibe la peticion, llama a UN metodo de
 * {@link EmpresaService}, devuelve la respuesta. Ninguna regla de
 * negocio vive aqui; si algo falla, la excepcion la lanza el
 * {@code service} y la traduce {@code GlobalExceptionHandler} (Etapa 7)
 * -- este controller nunca usa {@code try/catch}.</p>
 */
@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @PostMapping
    public ResponseEntity<EmpresaResponseDTO> crear(@Valid @RequestBody EmpresaRequestDTO dto) {
        EmpresaResponseDTO creada = empresaService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(empresaService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<EmpresaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(empresaService.listarTodas());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpresaResponseDTO> actualizar(@PathVariable Integer id,
                                                          @Valid @RequestBody EmpresaRequestDTO dto) {
        return ResponseEntity.ok(empresaService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<EmpresaResponseDTO> cambiarEstado(@PathVariable Integer id,
                                                             @Valid @RequestBody CambioEstadoRequestDTO dto) {
        return ResponseEntity.ok(empresaService.cambiarEstado(id, dto.nuevoEstado()));
    }
}
