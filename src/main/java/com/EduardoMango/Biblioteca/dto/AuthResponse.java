package com.EduardoMango.Biblioteca.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}

