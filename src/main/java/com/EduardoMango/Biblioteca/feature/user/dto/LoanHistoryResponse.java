package com.EduardoMango.Biblioteca.feature.user.dto;

import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;

import java.time.LocalDate;
import java.util.UUID;

public record LoanHistoryResponse(
        UUID publicId,
        String tituloLibro,
        LocalDate fechaPrestamo,
        LocalDate fechaDevolucionEsperada,
        LocalDate fechaDevolucionEfectiva,
        LoanStatus estado
) {
}

