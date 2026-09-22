package com.EduardoMango.Biblioteca.feature.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud para confirmar y activar una cuenta de usuario mediante token de verificación")
public record ConfirmAccountRequest(
        @Schema(description = "Token de verificación recibido en el correo tras el registro (TTL 15 min)",
                example = "e2b7a9f1-4c8d-4a35-b210-9876543210ab",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El token no puede estar vacío")
        String token
) {
}
