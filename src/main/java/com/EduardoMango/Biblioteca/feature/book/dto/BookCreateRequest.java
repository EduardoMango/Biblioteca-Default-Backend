package com.EduardoMango.Biblioteca.feature.book.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BookCreateRequest(
        @NotBlank(message = "El ISBN es obligatorio")
        String isbn,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título no puede exceder 200 caracteres")
        String titulo,

        @NotNull(message = "El stock total es obligatorio")
        @Min(value = 0, message = "El stock total no puede ser negativo")
        Integer stockTotal,

        @NotNull(message = "La categoría es obligatoria")
        UUID categoriaPublicId,

        @NotEmpty(message = "Debe asignar al menos un autor")
        List<UUID> autoresPublicIds
) {
}

