package com.EduardoMango.Biblioteca.feature.author.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AuthorResponse(
        UUID publicId,
        String nombre,
        String apellido,
        String nacionalidad,
        LocalDate fechaNacimiento
) {
}

