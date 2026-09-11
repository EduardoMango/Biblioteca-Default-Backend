package com.EduardoMango.Biblioteca.feature.book.dto;

import jakarta.validation.constraints.Size;

public record BookCoverUpdateRequest(
        @Size(max = 1000, message = "La URL de la portada no puede exceder 1000 caracteres")
        String urlPortada
) {
}

