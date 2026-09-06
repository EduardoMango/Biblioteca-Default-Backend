package com.EduardoMango.Biblioteca.feature.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}

