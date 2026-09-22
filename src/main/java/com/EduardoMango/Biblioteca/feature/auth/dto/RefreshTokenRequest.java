package com.EduardoMango.Biblioteca.feature.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud para refrescar y rotar el par de tokens JWT")
public record RefreshTokenRequest(
        @Schema(description = "Refresh token válido previamente emitido",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqdWFuLnBlcmV6IiwidHlwZSI6InJlZnJlc2giLCJpYXQiOjE3MDUwMDAwMDAsImV4cCI6MTcwNTYwNDgwMH0...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El refresh token no puede estar vacío")
        String refreshToken
) {
}
