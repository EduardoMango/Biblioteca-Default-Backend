package com.EduardoMango.Biblioteca.feature.loan.dto;

import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;

import java.time.LocalDate;
import java.util.UUID;

public record LoanResponse(
        UUID publicId,
        String libroIsbn,
        String libroTitulo,
        UUID usuarioPublicId,
        LocalDate fechaPrestamo,
        LocalDate fechaDevolucionEsperada,
        LocalDate fechaDevolucionEfectiva,
        LoanStatus estado
) {
}

