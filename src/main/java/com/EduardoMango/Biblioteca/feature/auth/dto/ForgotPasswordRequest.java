package com.EduardoMango.Biblioteca.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "El email no puede estar vacío")
        @Email(message = "El formato de email no es válido")
        String email
) {
}

