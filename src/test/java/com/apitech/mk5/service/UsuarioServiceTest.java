package com.apitech.mk5.service;

import com.apitech.mk5.dto.request.UsuarioRequestDTO;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.usuario.Rol;
import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.exception.ReglaDeNegocioException;
import com.apitech.mk5.mapper.UsuarioMapper;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.repository.usuario.RolRepository;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link UsuarioService}.
 *
 * <p>Se usan Mocks de Mockito para aislar el service de la base
 * de datos. Cada test verifica UNA regla de negocio específica.</p>
 *
 * <h3>Cobertura de reglas de negocio (ítem 4 lista de chequeo):</h3>
 * <ul>
 *   <li>Admin_ApiTech no puede tener empresa.</li>
 *   <li>Admin_Cliente debe tener empresa.</li>
 *   <li>Correo duplicado lanza excepción.</li>
 *   <li>Creación exitosa guarda el usuario con hash de contraseña.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioServiceImpl — pruebas unitarias")
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private com.apitech.mk5.service.impl.UsuarioServiceImpl usuarioService;

    private Rol rolAdminApiTech;
    private Rol rolAdminCliente;
    private Empresa empresaPrueba;

    @BeforeEach
    void setUp() {
        rolAdminApiTech = new Rol("Admin_ApiTech", "Admin global");
        rolAdminCliente = new Rol("Admin_Cliente", "Admin empresa");
        empresaPrueba   = new Empresa("900-1", "El Panal", null, null, null, "activa");
    }

    // ── Test 1: Admin_ApiTech con empresa → excepción ────────────
    @Test
    @DisplayName("Admin_ApiTech con empresa asignada debe lanzar ReglaDeNegocioException")
    void adminApiTech_conEmpresa_lanzaExcepcion() {
        // Arrange
        UsuarioRequestDTO dto = new UsuarioRequestDTO(
                1,              // idEmpresa (NO debería tener)
                1,              // idRol = Admin_ApiTech
                "Juan", "Pérez",
                "juan@apitech.com",
                "password123",
                "activo");

        when(usuarioRepository.existsByCorreo(dto.correo())).thenReturn(false);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolAdminApiTech));
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresaPrueba));

        // Act + Assert
        assertThatThrownBy(() -> usuarioService.crear(dto))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Admin_ApiTech");
    }

    // ── Test 2: Admin_Cliente sin empresa → excepción ───────────
    @Test
    @DisplayName("Admin_Cliente sin empresa debe lanzar ReglaDeNegocioException")
    void adminCliente_sinEmpresa_lanzaExcepcion() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO(
                null,           // sin empresa
                3,              // idRol = Admin_Cliente
                "María", "López",
                "maria@elpanal.com",
                "password123",
                "activo");

        when(usuarioRepository.existsByCorreo(dto.correo())).thenReturn(false);
        when(rolRepository.findById(3)).thenReturn(Optional.of(rolAdminCliente));

        assertThatThrownBy(() -> usuarioService.crear(dto))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Admin_Cliente");
    }

    // ── Test 3: Correo duplicado → excepción ────────────────────
    @Test
    @DisplayName("Correo duplicado debe lanzar ReglaDeNegocioException")
    void correo_duplicado_lanzaExcepcion() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO(
                null, 1, "Juan", "Test",
                "admin@apitech.com", "password123", "activo");

        when(usuarioRepository.existsByCorreo("admin@apitech.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(dto))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("admin@apitech.com");
    }

    // ── Test 4: Creación exitosa ─────────────────────────────────
    @Test
    @DisplayName("Creación exitosa de Admin_ApiTech sin empresa")
    void crear_adminApiTech_sinEmpresa_exitoso() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO(
                null,          // sin empresa
                1,             // Admin_ApiTech
                "Admin", "Tech",
                "nuevo@apitech.com",
                "password123",
                "activo");

        when(usuarioRepository.existsByCorreo(dto.correo())).thenReturn(false);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolAdminApiTech));
        when(passwordEncoder.encode("password123")).thenReturn("$2y$hash");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var resultado = usuarioService.crear(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.correo()).isEqualTo("nuevo@apitech.com");
        assertThat(resultado.empresa()).isNull();
        verify(usuarioRepository).save(any(Usuario.class));
    }

    // ── Test 5: Contraseña nunca se devuelve en el DTO ──────────
    @Test
    @DisplayName("El DTO de respuesta no expone la contraseña")
    void respuesta_noExpone_password() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO(
                null, 1, "Admin", "Tech",
                "admin2@apitech.com", "miClaveSecreta", "activo");

        when(usuarioRepository.existsByCorreo(dto.correo())).thenReturn(false);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolAdminApiTech));
        when(passwordEncoder.encode(anyString())).thenReturn("$2y$hash");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var resultado = usuarioService.crear(dto);

        // UsuarioResponseDTO no tiene campo password
        // Verificamos que el objeto resultado no tenga ningún campo
        // que contenga la clave (reflexión implícita por el tipo record)
        assertThat(resultado.toString()).doesNotContain("miClaveSecreta");
    }
}