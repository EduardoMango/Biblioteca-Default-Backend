package com.EduardoMango.Biblioteca.feature.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales requeridas para autenticar un usuario en el sistema")
public record AuthRequest(
        @Schema(description = "Nombre de usuario registrado", example = "juan.perez", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre de usuario no puede estar vacío")
        String username,

        @Schema(description = "Contraseña en texto plano", example = "Secret123*", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña no puede estar vacía")
        String password
) {
}
