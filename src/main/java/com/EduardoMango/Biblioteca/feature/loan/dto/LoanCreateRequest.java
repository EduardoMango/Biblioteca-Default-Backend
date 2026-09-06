package com.EduardoMango.Biblioteca.feature.loan.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LoanCreateRequest(
        @NotNull(message = "El publicId del libro es obligatorio")
        UUID libroPublicId,
        UUID usuarioPublicId
) {
}

