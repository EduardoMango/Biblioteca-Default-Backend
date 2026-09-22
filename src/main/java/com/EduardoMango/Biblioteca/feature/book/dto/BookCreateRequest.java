package com.EduardoMango.Biblioteca.feature.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Datos para la creación manual de un nuevo libro en el catálogo")
public record BookCreateRequest(
        @Schema(description = "Código ISBN único de 10 o 13 dígitos", example = "9780307474728", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El ISBN es obligatorio")
        String isbn,

        @Schema(description = "Título principal de la obra", example = "Cien años de soledad", maxLength = 200, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título no puede exceder 200 caracteres")
        String titulo,

        @Schema(description = "URL accesible de la imagen de portada", example = "https://images.example.com/portadas/9780307474728.jpg", maxLength = 1000, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 1000, message = "La URL de la portada no puede exceder 1000 caracteres")
        String urlPortada,

        @Schema(description = "Cantidad total inicial de copias físicas disponibles para inventario", example = "5", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El stock total es obligatorio")
        @Min(value = 0, message = "El stock total no puede ser negativo")
        Integer stockTotal,

        @Schema(description = "Identificador público de la categoría a la que pertenece el libro", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La categoría es obligatoria")
        UUID categoriaPublicId,

        @Schema(description = "Lista de identificadores públicos de los autores que escribieron la obra", example = "[\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Debe asignar al menos un autor")
        List<UUID> autoresPublicIds,

        @Schema(description = "Casa editorial responsable de la publicación", example = "Editorial Sudamericana", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String editorial,

        @Schema(description = "Sinopsis o descripción del contenido del libro", example = "Novela cumbre del realismo mágico latinoamericano...", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String descripcion
) {
    public BookCreateRequest(String isbn, String titulo, Integer stockTotal, UUID categoriaPublicId, List<UUID> autoresPublicIds) {
        this(isbn, titulo, null, stockTotal, categoriaPublicId, autoresPublicIds, null, null);
    }

    public BookCreateRequest(String isbn, String titulo, String urlPortada, Integer stockTotal, UUID categoriaPublicId, List<UUID> autoresPublicIds) {
        this(isbn, titulo, urlPortada, stockTotal, categoriaPublicId, autoresPublicIds, null, null);
    }
}
