package com.apitech.mk5.controller;

import com.apitech.mk5.dto.request.ColmenaRequestDTO;
import com.apitech.mk5.dto.response.ColmenaResponseDTO;
import com.apitech.mk5.dto.response.EmpresaResponseDTO;
import com.apitech.mk5.service.ColmenaService;
import com.apitech.mk5.security.SecurityConfig;
import com.apitech.mk5.security.UsuarioDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración del {@link ColmenaController}.
 *
 * <p>{@code @WebMvcTest} carga solo la capa web (controladores,
 * filtros de seguridad, serializadores JSON) sin levantar la
 * base de datos. Es más rápido que {@code @SpringBootTest} y
 * suficiente para validar que los endpoints respondan
 * correctamente con el formato esperado.</p>
 *
 * <p>{@code @WithMockUser} simula un usuario autenticado para
 * las pruebas, sin necesitar el flujo de login real.</p>
 */
@WebMvcTest(ColmenaController.class)
@Import(SecurityConfig.class)
@DisplayName("ColmenaController — pruebas de integración web")
class ColmenaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private ColmenaService colmenaService;
    @MockBean  private UsuarioDetailsService usuarioDetailsService;

    private ColmenaResponseDTO colmenaRespuesta() {
        EmpresaResponseDTO empresa = new EmpresaResponseDTO(
                1, "900-1", "El Panal", null, null, null,
                "activa", LocalDateTime.now(), LocalDateTime.now());
        return new ColmenaResponseDTO(
                1, empresa, null,
                "Colmena C001", "Sector Norte", "Estable",
                LocalDateTime.now(), LocalDateTime.now());
    }

    // ── GET /api/colmenas?idEmpresa=1 ────────────────────────────
    @Test
    @WithMockUser(roles = "Admin_Cliente")
    @DisplayName("GET /api/colmenas?idEmpresa=1 retorna 200 con lista")
    void listar_retorna200() throws Exception {
        when(colmenaService.listarPorEmpresa(1))
                .thenReturn(List.of(colmenaRespuesta()));

        mockMvc.perform(get("/api/colmenas").param("idEmpresa", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Colmena C001"))
                .andExpect(jsonPath("$[0].estado").value("Estable"));
    }

    // ── POST /api/colmenas ───────────────────────────────────────
    @Test
    @WithMockUser(roles = "Admin_Cliente")
    @DisplayName("POST /api/colmenas con datos válidos retorna 201")
    void crear_retorna201() throws Exception {
        ColmenaRequestDTO dto = new ColmenaRequestDTO(
                1, null, "Colmena Nueva", "Terraza B", "Estable");

        when(colmenaService.crear(any(ColmenaRequestDTO.class)))
                .thenReturn(colmenaRespuesta());

        mockMvc.perform(post("/api/colmenas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Colmena C001"));
    }

    // ── POST con nombre vacío → 400 ──────────────────────────────
    @Test
    @WithMockUser(roles = "Admin_Cliente")
    @DisplayName("POST con nombre vacío retorna 400 Bad Request")
    void crear_nombreVacio_retorna400() throws Exception {
        ColmenaRequestDTO dto = new ColmenaRequestDTO(
                1, null, "", "Terraza B", "Estable");

        mockMvc.perform(post("/api/colmenas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Uno o mas campos no son validos"));
    }

    // ── Sin autenticación → 401/302 ──────────────────────────────
    @Test
    @DisplayName("Sin autenticación retorna 302 redirect al login")
    void sinAuth_redirigeLogin() throws Exception {
        mockMvc.perform(get("/api/colmenas").param("idEmpresa", "1"))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() == 401
                                || result.getResponse().getStatus() / 100 == 3));
    }

    // ── Empleado_Cliente no puede crear ─────────────────────────
    @Test
    @WithMockUser(roles = "Empleado_Cliente")
    @DisplayName("Empleado_Cliente no puede crear colmenas (403)")
    void empleadoCliente_noPuedeCrear_retorna403() throws Exception {
        ColmenaRequestDTO dto = new ColmenaRequestDTO(
                1, null, "Colmena Test", null, "Estable");

        mockMvc.perform(post("/api/colmenas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }
}
