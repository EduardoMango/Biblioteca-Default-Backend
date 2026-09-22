package com.EduardoMango.Biblioteca.feature.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud para reenviar el correo con token de activación de cuenta")
public record ResendVerificationRequest(
        @Schema(description = "Correo electrónico de la cuenta pendiente de activación",
                example = "usuario@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El email no puede estar vacío")
        @Email(message = "El formato de email no es válido")
        String email
) {
}
