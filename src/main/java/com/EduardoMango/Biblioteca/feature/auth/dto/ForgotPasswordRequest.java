package com.EduardoMango.Biblioteca.feature.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud de envío de correo para restablecimiento de contraseña")
public record ForgotPasswordRequest(
        @Schema(description = "Correo electrónico asociado a la cuenta registrada", example = "usuario@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El email no puede estar vacío")
        @Email(message = "El formato de email no es válido")
        String email
) {
}
