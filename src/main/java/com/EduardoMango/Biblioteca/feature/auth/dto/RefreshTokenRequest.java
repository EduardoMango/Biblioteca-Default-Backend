package com.EduardoMango.Biblioteca.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "El refresh token no puede estar vacío")
        String refreshToken
) {
}

