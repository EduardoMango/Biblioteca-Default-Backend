package com.EduardoMango.Biblioteca.feature.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede exceder los 255 caracteres")
        String descripcion
) {
}

