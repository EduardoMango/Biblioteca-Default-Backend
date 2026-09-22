package com.EduardoMango.Biblioteca.feature.auth.dto;

import com.EduardoMango.Biblioteca.feature.auth.domain.Roles;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para el autoregistro de un nuevo usuario socio en la biblioteca")
public record RegisterRequest(
        @Schema(description = "Nombre de usuario único para acceso al sistema", example = "maria.gonzalez", minLength = 3, maxLength = 50, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        String username,

        @Schema(description = "Contraseña en texto plano que será hasheada con BCrypt", example = "Password2026*", minLength = 6, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @Schema(description = "Nombre de pila del usuario", example = "María", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @Schema(description = "Apellido del usuario", example = "González", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El apellido es obligatorio")
        String apellido,

        @Schema(description = "Correo electrónico para notificaciones y activación de cuenta", example = "maria.gonzalez@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de correo electrónico inválido")
        String email,

        @Schema(description = "Documento Nacional de Identidad (opcional)", example = "40123456", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String dni,

        @Schema(description = "Teléfono de contacto (opcional)", example = "+54 11 5555-1234", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String telefono,

        @Schema(description = "Rol solicitado para la cuenta (por defecto SOCIO)", example = "SOCIO", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Roles role
) {
}
