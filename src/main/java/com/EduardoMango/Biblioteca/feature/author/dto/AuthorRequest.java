package com.EduardoMango.Biblioteca.feature.author.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AuthorRequest(
        @NotBlank(message = "El nombre no puede estar vacío")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido no puede estar en blanco")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String apellido,

        @Size(max = 100, message = "La nacionalidad no puede exceder 100 caracteres")
        String nacionalidad,

        LocalDate fechaNacimiento
) {
}

