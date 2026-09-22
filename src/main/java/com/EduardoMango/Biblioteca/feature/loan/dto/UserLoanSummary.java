package com.EduardoMango.Biblioteca.feature.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resumen de información básica del socio asociado a un préstamo")
public record UserLoanSummary(
        @Schema(description = "Identificador público del usuario", example = "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d")
        UUID publicId,

        @Schema(description = "Nombre completo del socio", example = "Juan Pérez")
        String nombre,

        @Schema(description = "Correo electrónico del socio", example = "juan.perez@example.com")
        String email
) {
}
