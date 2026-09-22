package com.EduardoMango.Biblioteca.feature.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos requeridos para restablecer la contraseña mediante token de recuperación")
public record ResetPasswordRequest(
        @Schema(description = "Token temporal de recuperación recibido por correo electrónico (TTL 15 min)",
                example = "9f4c3a28-6e7b-481d-b532-a63e9f451234",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El token no puede estar vacío")
        String token,

        @Schema(description = "Nueva contraseña en texto plano (mínimo 6 caracteres)",
                example = "NuevaPass2026*",
                minLength = 6,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña no puede estar vacía")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String newPassword
) {
}
