package com.EduardoMango.Biblioteca.feature.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Representación del usuario para gestión administrativa y auditoría")
public record UserAdminResponse(
        @Schema(description = "Identificador público inmutable del usuario", example = "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d")
        UUID publicId,

        @Schema(description = "Nombre de pila", example = "Juan")
        String nombre,

        @Schema(description = "Apellido", example = "Pérez")
        String apellido,

        @Schema(description = "Correo electrónico", example = "juan.perez@example.com")
        String email,

        @Schema(description = "Rol asignado en el sistema", example = "ROLE_SOCIO")
        String rol,

        @Schema(description = "Indica si la cuenta está activa para operar en la biblioteca", example = "true")
        Boolean activo
) {
}
