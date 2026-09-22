package com.EduardoMango.Biblioteca.feature.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para actualizar específicamente la portada de un libro")
public record BookCoverUpdateRequest(
        @Schema(description = "Nueva URL de la imagen de portada", example = "https://images.example.com/portadas/portada_hd.png", maxLength = 1000)
        @Size(max = 1000, message = "La URL de la portada no puede exceder 1000 caracteres")
        String urlPortada
) {
}
