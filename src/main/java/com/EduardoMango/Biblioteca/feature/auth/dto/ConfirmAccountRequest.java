package com.EduardoMango.Biblioteca.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmAccountRequest(
        @NotBlank(message = "El token no puede estar vacío")
        String token
) {
}

