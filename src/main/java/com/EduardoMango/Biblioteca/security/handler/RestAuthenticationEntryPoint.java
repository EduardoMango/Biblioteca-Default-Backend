package com.EduardoMango.Biblioteca.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        response.setContentType("application/problem+json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String errorMessage = switch (authException) {
            case BadCredentialsException badCredentialsException ->
                    "Credenciales inválidas";
            case DisabledException disabledException ->
                    "Cuenta deshabilitada";
            case LockedException lockedException ->
                    "Cuenta bloqueada";
            case AccountExpiredException accountExpiredException ->
                    "Cuenta expirada";
            case CredentialsExpiredException credentialsExpiredException ->
                    "Credenciales expiradas";
            case InsufficientAuthenticationException insufficientAuthenticationException ->
                    "Autenticación requerida o insuficiente";
            case AuthenticationServiceException authenticationServiceException ->
                    "Error en el servicio de autenticación";
            default -> "Error de autenticación: " + authException.getMessage();
        };

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", "Unauthorized");
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("detail", errorMessage);
        body.put("instance", request.getRequestURI());
        body.put("error", errorMessage);
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("path", request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
