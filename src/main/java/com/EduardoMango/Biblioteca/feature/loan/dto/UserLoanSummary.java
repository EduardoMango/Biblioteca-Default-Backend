package com.EduardoMango.Biblioteca.feature.loan.dto;

import java.util.UUID;

public record UserLoanSummary(
        UUID publicId,
        String nombre,
        String email
) {
}

