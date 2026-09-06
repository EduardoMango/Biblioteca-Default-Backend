package com.EduardoMango.Biblioteca.feature.loan.dto;

import java.util.UUID;

public record BookLoanSummary(
        UUID publicId,
        String titulo,
        String isbn
) {
}

