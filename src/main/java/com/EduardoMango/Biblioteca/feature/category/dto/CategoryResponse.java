package com.EduardoMango.Biblioteca.feature.category.dto;

import java.util.UUID;

public record CategoryResponse(
        UUID publicId,
        String nombre,
        String descripcion
) {
}

