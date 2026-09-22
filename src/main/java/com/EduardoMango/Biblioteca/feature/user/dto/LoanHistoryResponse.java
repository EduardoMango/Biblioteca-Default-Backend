package com.EduardoMango.Biblioteca.feature.user.dto;

import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Registro de préstamo perteneciente al historial personal del socio")
public record LoanHistoryResponse(
        @Schema(description = "Identificador público del préstamo", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID publicId,

        @Schema(description = "Título de la obra prestada", example = "Cien años de soledad")
        String tituloLibro,

        @Schema(description = "Fecha en que se retiró el ejemplar", example = "2026-09-01")
        LocalDate fechaPrestamo,

        @Schema(description = "Fecha límite esperada para la devolución", example = "2026-09-15")
        LocalDate fechaDevolucionEsperada,

        @Schema(description = "Fecha en que se efectuó la devolución (null si continúa activo)", example = "2026-09-14")
        LocalDate fechaDevolucionEfectiva,

        @Schema(description = "Estado del préstamo", example = "DEVUELTO")
        LoanStatus estado
) {
}
