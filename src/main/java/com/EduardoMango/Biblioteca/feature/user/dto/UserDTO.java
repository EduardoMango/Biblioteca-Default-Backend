package com.EduardoMango.Biblioteca.feature.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Datos públicos del usuario registrado exitosamente en el sistema")
public record UserDTO(
        @Schema(description = "Identificador público inmutable del usuario", example = "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d")
        UUID publicId,

        @Schema(description = "Nombre de usuario", example = "maria.gonzalez")
        String username,

        @Schema(description = "Nombre de pila", example = "María")
        String nombre,

        @Schema(description = "Apellido", example = "González")
        String apellido,

        @Schema(description = "Correo electrónico registrado", example = "maria.gonzalez@example.com")
        String email,

        @Schema(description = "Documento Nacional de Identidad", example = "40123456")
        String dni,

        @Schema(description = "Teléfono de contacto", example = "+54 11 5555-1234")
        String telefono,

        @Schema(description = "Conjunto de roles asignados", example = "[\"ROLE_SOCIO\"]")
        Set<String> roles
) {
}
