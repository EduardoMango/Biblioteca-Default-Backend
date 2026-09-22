package com.EduardoMango.Biblioteca.feature.loan.dto;

import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Vista consolidada para la supervisión y auditoría administrativa de préstamos")
public record LoanSupervisionResponse(
        @Schema(description = "Identificador público del préstamo", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID publicId,

        @Schema(description = "Datos resumidos del socio prestatario")
        UserLoanSummary usuario,

        @Schema(description = "Datos resumidos del libro prestado")
        BookLoanSummary libro,

        @Schema(description = "Fecha de emisión del préstamo", example = "2026-09-01")
        LocalDate fechaPrestamo,

        @Schema(description = "Fecha pactada para la devolución", example = "2026-09-15")
        LocalDate fechaDevolucionEsperada,

        @Schema(description = "Fecha en que se efectuó la devolución física (null si continúa activo)", example = "2026-09-18")
        LocalDate fechaDevolucionEfectiva,

        @Schema(description = "Estado administrativo del préstamo", example = "CON_RETRASO")
        LoanStatus estado,

        @Schema(description = "Días transcurridos fuera de término", example = "3")
        Long diasAtraso
) {
    public UUID getUsuarioPublicId() {
        return usuario != null ? usuario.publicId() : null;
    }

    public String getLibroIsbn() {
        return libro != null ? libro.isbn() : null;
    }
}
