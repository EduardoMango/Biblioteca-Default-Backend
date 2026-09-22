package com.EduardoMango.Biblioteca.feature.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Perfil detallado del usuario autenticado actualmente")
public record UserProfileResponse(
        @Schema(description = "Identificador público inmutable del usuario", example = "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d")
        UUID publicId,

        @Schema(description = "Nombre de pila", example = "Juan")
        String nombre,

        @Schema(description = "Apellido", example = "Pérez")
        String apellido,

        @Schema(description = "Correo electrónico de la cuenta", example = "juan.perez@example.com")
        String email,

        @Schema(description = "Rol institucional principal", example = "ROLE_SOCIO")
        String rol,

        @Schema(description = "Lista resumida de los préstamos más recientes solicitados por el usuario")
        List<LoanHistoryResponse> prestamos
) {
}
