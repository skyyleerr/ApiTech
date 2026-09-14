package com.apitech.mk5.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.http.HttpMethod;

/**
 * Configuración central de Spring Security.
 *
 * <h3>Roles y accesos:</h3>
 * <ul>
 *   <li>{@code Admin_ApiTech} — acceso total, incluyendo panel
 *       de empresas, suscripciones y pagos.</li>
 *   <li>{@code Empleado_ApiTech} — acceso de consulta a empresas
 *       y pagos, sin poder modificar.</li>
 *   <li>{@code Admin_Cliente} — acceso a colmenas, sensores,
 *       monitoreo, alertas, producción y reportes de su empresa.</li>
 *   <li>{@code Empleado_Cliente} — acceso de solo lectura a
 *       colmenas, sensores, mediciones, alertas y producción.</li>
 * </ul>
 *
 * <p>{@code @EnableMethodSecurity} activa las anotaciones
 * {@code @PreAuthorize} y {@code @PostAuthorize} en servicios
 * y controladores, permitiendo control de acceso a nivel de método
 * (más granular que solo por URL).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .authenticationProvider(authenticationProvider())

            // ── Autorización por URL ─────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Recursos estáticos y login: públicos
                .requestMatchers(
                    "/css/**", "/js/**", "/img/**", "/favicon.ico",
                    "/", "/login", "/login/empresa", "/planes", "/solicitud-suscripcion", "/error"
                ).permitAll()

                // API REST: requiere autenticación (se usa desde Postman o JS)
                .requestMatchers("/api/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/colmenas/**")
                    .hasRole("Admin_Cliente")
                .requestMatchers(HttpMethod.PUT, "/api/colmenas/**")
                    .hasRole("Admin_Cliente")
                .requestMatchers(HttpMethod.DELETE, "/api/colmenas/**")
                    .hasRole("Admin_Cliente")

                // Gestión de solicitudes: ambos roles internos pueden revisar y procesar.
                .requestMatchers("/admin/solicitudes/**")
                    .hasAnyRole("Admin_ApiTech", "Empleado_ApiTech")

                // Gestión de empresas: solo Admin_ApiTech
                .requestMatchers("/admin/empresas/crear",
                                 "/admin/empresas/editar/**",
                                 "/admin/empresas/estado/**")
                    .hasRole("Admin_ApiTech")

                // Verificación de pagos: ambos roles internos pueden verificar.
                .requestMatchers("/admin/pagos/verificar/**",
                                 "/admin/pagos/rechazar/**")
                    .hasAnyRole("Admin_ApiTech", "Empleado_ApiTech")

                // Panel de ApiTech: solo equipo interno
                .requestMatchers("/admin/**")
                    .hasAnyRole("Admin_ApiTech", "Empleado_ApiTech")

                // Panel de cliente (colmenas, sensores, etc.)
                .requestMatchers("/app/**")
                    .hasAnyRole("Admin_Cliente", "Empleado_Cliente")

                // Acciones de administración de cliente
                .requestMatchers("/app/colmenas/crear",
                                 "/app/colmenas/editar/**",
                                 "/app/sensores/registrar",
                                 "/app/monitoreo/**",
                                 "/app/produccion/registrar")
                    .hasRole("Admin_Cliente")

                // Reportes: admin cliente puede generar y exportar
                .requestMatchers("/app/reportes/**")
                    .hasAnyRole("Admin_Cliente", "Empleado_Cliente")

                .requestMatchers("/app/pagos/**")
                    .hasRole("Admin_Cliente")

                .requestMatchers("/admin/pagos/**")
                    .hasAnyRole("Admin_ApiTech", "Empleado_ApiTech")

                // Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )

            // ── Formulario de login ──────────────────────────
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("correo")
                .passwordParameter("password")
                // Redirige según el rol después del login
                .successHandler(new RolBasedLoginSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )

            // ── Logout ───────────────────────────────────────
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            .exceptionHandling(exceptions -> exceptions
                    .defaultAuthenticationEntryPointFor(
                            new LoginUrlAuthenticationEntryPoint("/login"),
                            new AntPathRequestMatcher("/api/**")))

            // El formulario de autenticación no depende de una sesión previa.
            // El resto de formularios Thymeleaf conserva protección CSRF.
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    // La API se consume desde Postman/clientes externos y no
                    // dispone de la sesión HTML necesaria para transportar CSRF.
                    new AntPathRequestMatcher("/api/**"),
                    new AntPathRequestMatcher("/login")))

            // CSRF se exime para REST externo; permanece activo en las vistas
            // Thymeleaf (sus formularios incluyen el token automáticamente).
            ;

        return http.build();
    }
}
