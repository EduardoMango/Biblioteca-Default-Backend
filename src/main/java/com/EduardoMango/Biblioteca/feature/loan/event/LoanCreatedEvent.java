package com.EduardoMango.Biblioteca.feature.loan.event;

import java.time.LocalDate;
import java.util.UUID;

public record LoanCreatedEvent(
        UUID loanPublicId,
        String userEmail,
        String userName,
        String bookTitle,
        LocalDate fechaPrestamo,
        LocalDate fechaDevolucionEsperada
) {
}

