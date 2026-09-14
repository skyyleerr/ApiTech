package com.apitech.mk5.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Redirige al dashboard correcto después del login según el rol.
 *
 * <ul>
 *   <li>Admin_ApiTech / Empleado_ApiTech → /admin/dashboard</li>
 *   <li>Admin_Cliente / Empleado_Cliente → /app/dashboard</li>
 * </ul>
 */
public class RolBasedLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        String redirectUrl = "/app/dashboard"; // default

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String rol = authority.getAuthority();
            if (rol.equals("ROLE_Admin_ApiTech")
                    || rol.equals("ROLE_Empleado_ApiTech")) {
                redirectUrl = "/admin/dashboard";
                break;
            }
        }

        // El NIT seleccionado solo sirve durante la autenticación. Una vez
        // autenticado, el tenant real se obtiene siempre desde Usuario.
        if (redirectUrl.equals("/admin/dashboard")) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute("LOGIN_EMPRESA_ID");
                session.removeAttribute("LOGIN_EMPRESA_NIT");
            }
        }
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}