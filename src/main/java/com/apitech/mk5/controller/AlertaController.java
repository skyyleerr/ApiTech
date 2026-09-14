package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.AlertaRequestDTO;
import com.apitech.mk5.dto.response.AlertaResponseDTO;
import com.apitech.mk5.service.AlertaService;
import com.apitech.mk5.security.UsuarioContexto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaService alertaService;
    private final UsuarioContexto usuarioContexto;

    public AlertaController(AlertaService alertaService, UsuarioContexto usuarioContexto) {
        this.alertaService = alertaService;
        this.usuarioContexto = usuarioContexto;
    }

    /**
     * Crea una alerta. {@code idEmpresa} NO se recibe aqui -- el
     * service lo deriva automaticamente de la colmena (ver
     * {@link AlertaService#crear}), replicando {@code trg_alertas_empresa_bi}.
     */
    @PostMapping
    public ResponseEntity<AlertaResponseDTO> crear(@Valid @RequestBody AlertaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertaService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertaResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(alertaService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<AlertaResponseDTO>> listar(@RequestParam Integer idEmpresa,
                                                           @RequestParam String estado,
                                                           Pageable pageable) {
        return ResponseEntity.ok(alertaService.listarPorEmpresaYEstado(idEmpresa, estado, pageable));
    }

    @GetMapping("/activas")
    public ResponseEntity<Page<AlertaResponseDTO>> activas() {
        return ResponseEntity.ok(alertaService.listarPorEmpresaYEstado(
                usuarioContexto.getIdEmpresaActual(), "ACTIVA", Pageable.unpaged()));
    }

    @GetMapping("/colmena/{idColmena}")
    public ResponseEntity<Page<AlertaResponseDTO>> listarPorColmena(@PathVariable Integer idColmena,
                                                                     Pageable pageable) {
        return ResponseEntity.ok(alertaService.listarPorColmena(idColmena, pageable));
    }

    @PatchMapping("/{id}/resolver")
    public ResponseEntity<AlertaResponseDTO> resolver(@PathVariable Integer id,
                                                       @RequestParam Integer idUsuarioResolucion) {
        return ResponseEntity.ok(alertaService.resolver(id, idUsuarioResolucion));
    }
}
