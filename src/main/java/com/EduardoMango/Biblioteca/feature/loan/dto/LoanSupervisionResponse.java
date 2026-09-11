package com.EduardoMango.Biblioteca.feature.loan.dto;

import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;

import java.time.LocalDate;
import java.util.UUID;

public record LoanSupervisionResponse(
        UUID publicId,
        UserLoanSummary usuario,
        BookLoanSummary libro,
        LocalDate fechaPrestamo,
        LocalDate fechaDevolucionEsperada,
        LocalDate fechaDevolucionEfectiva,
        LoanStatus estado,
        Long diasAtraso
) {
    public UUID getUsuarioPublicId() {
        return usuario != null ? usuario.publicId() : null;
    }

    public String getLibroIsbn() {
        return libro != null ? libro.isbn() : null;
    }
}

