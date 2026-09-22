package com.EduardoMango.Biblioteca.feature.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de autenticación con el par de tokens JWT emitidos")
public record AuthResponse(
        @Schema(description = "Access Token JWT para autorizar solicitudes subsiguientes en el header Authorization: Bearer",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqdWFuLnBlcmV6Iiwicm9sZXMiOlsicm9sZV9zb2NpbyJdLCJpYXQiOjE3MDUwMDAwMDAsImV4cCI6MTcwNTAwMDkwMH0...")
        String accessToken,

        @Schema(description = "Refresh Token JWT persistido para renovar el access token sin reenviar credenciales",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqdWFuLnBlcmV6IiwidHlwZSI6InJlZnJlc2giLCJpYXQiOjE3MDUwMDAwMDAsImV4cCI6MTcwNTYwNDgwMH0...")
        String refreshToken
) {
}
