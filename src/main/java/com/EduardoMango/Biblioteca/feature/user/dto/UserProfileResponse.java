package com.EduardoMango.Biblioteca.feature.user.dto;

import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID publicId,
        String nombre,
        String apellido,
        String email,
        String rol,
        List<LoanHistoryResponse> prestamos
) {
}

