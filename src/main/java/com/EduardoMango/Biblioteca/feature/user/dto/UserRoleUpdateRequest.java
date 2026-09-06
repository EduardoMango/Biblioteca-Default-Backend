package com.EduardoMango.Biblioteca.feature.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserRoleUpdateRequest(
        @NotBlank(message = "El nuevo rol es obligatorio")
        String nuevoRol
) {
}

