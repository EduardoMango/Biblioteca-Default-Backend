package com.EduardoMango.Biblioteca.feature.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}

