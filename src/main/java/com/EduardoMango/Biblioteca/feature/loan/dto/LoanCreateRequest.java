package com.EduardoMango.Biblioteca.feature.loan.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record LoanCreateRequest(
        @NotBlank(message = "El ISBN del libro es obligatorio")
        String libroIsbn,
        UUID usuarioPublicId
) {
}

