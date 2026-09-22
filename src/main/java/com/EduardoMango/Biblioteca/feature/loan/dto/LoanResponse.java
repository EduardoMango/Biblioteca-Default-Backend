package com.EduardoMango.Biblioteca.feature.loan.dto;

import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Datos detallados de un préstamo y su estado en el ciclo de vida")
public record LoanResponse(
        @Schema(description = "Identificador público inmutable del préstamo", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID publicId,

        @Schema(description = "ISBN del libro prestado", example = "9780307474728")
        String libroIsbn,

        @Schema(description = "Título del libro prestado", example = "Cien años de soledad")
        String libroTitulo,

        @Schema(description = "Identificador público del socio prestatario", example = "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d")
        UUID usuarioPublicId,

        @Schema(description = "Fecha de emisión del préstamo", example = "2026-09-22")
        LocalDate fechaPrestamo,

        @Schema(description = "Fecha límite esperada para la devolución (14 días por defecto)", example = "2026-10-06")
        LocalDate fechaDevolucionEsperada,

        @Schema(description = "Fecha real en que se registró la devolución física", example = "2026-10-05")
        LocalDate fechaDevolucionEfectiva,

        @Schema(description = "Estado actual del préstamo (PRESTADO, DEVUELTO, CON_RETRASO)", example = "PRESTADO")
        LoanStatus estado
) {
}
