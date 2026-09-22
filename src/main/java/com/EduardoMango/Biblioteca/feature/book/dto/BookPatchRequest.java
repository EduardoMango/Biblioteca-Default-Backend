package com.EduardoMango.Biblioteca.feature.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para modificación parcial de campos no estructurales de un libro")
public record BookPatchRequest(
        @Schema(description = "URL de la portada del libro", example = "https://images.example.com/portadas/nuevo_diseño.jpg", maxLength = 1000)
        @Size(max = 1000, message = "La URL de la portada no puede exceder 1000 caracteres")
        String urlPortada
) {
}
