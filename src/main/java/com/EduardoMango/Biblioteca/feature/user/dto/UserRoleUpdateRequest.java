package com.EduardoMango.Biblioteca.feature.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud para modificar el rol asignado a un usuario")
public record UserRoleUpdateRequest(
        @Schema(description = "Nuevo rol institucional (SOCIO o BIBLIOTECARIO)",
                example = "BIBLIOTECARIO",
                allowableValues = {"SOCIO", "BIBLIOTECARIO", "ROLE_SOCIO", "ROLE_BIBLIOTECARIO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nuevo rol es obligatorio")
        String nuevoRol
) {
}
