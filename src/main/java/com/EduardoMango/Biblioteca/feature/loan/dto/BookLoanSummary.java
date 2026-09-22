package com.EduardoMango.Biblioteca.feature.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumen de información básica del libro asociado a un préstamo")
public record BookLoanSummary(
        @Schema(description = "Código ISBN del libro", example = "9780307474728")
        String isbn,

        @Schema(description = "Título de la obra", example = "Cien años de soledad")
        String titulo
) {
}
