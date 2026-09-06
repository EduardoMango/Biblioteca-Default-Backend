package com.EduardoMango.Biblioteca.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.setContentType("application/problem+json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        String errorMessage = "Acceso denegado: No posee los permisos o roles requeridos para este recurso";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", "Forbidden");
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("detail", errorMessage);
        body.put("instance", request.getRequestURI());
        body.put("error", errorMessage);
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("path", request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
