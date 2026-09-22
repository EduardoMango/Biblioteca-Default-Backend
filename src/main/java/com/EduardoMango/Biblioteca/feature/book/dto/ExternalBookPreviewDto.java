package com.EduardoMango.Biblioteca.feature.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Borrador de metadatos de un libro obtenido desde Google Books API sin persistir")
public record ExternalBookPreviewDto(
        @Schema(description = "Código ISBN consultado", example = "9780307474728")
        String isbn,

        @Schema(description = "Título obtenido desde el proveedor externo", example = "Cien años de soledad")
        String title,

        @Schema(description = "Lista de autores identificados externamente", example = "[\"Gabriel García Márquez\"]")
        List<String> authors,

        @Schema(description = "Editorial registrada externamente", example = "Vintage Espanol")
        String publisher,

        @Schema(description = "Sinopsis provista por la API externa", example = "La novela narra la historia de la familia Buendía a lo largo de siete generaciones...")
        String description,

        @Schema(description = "URL de la portada en alta resolución provista por Google Books", example = "http://books.google.com/books/content?id=xyz...")
        String coverUrl,

        @Schema(description = "Categorías o temas sugeridos", example = "[\"Fiction\", \"Magic Realism\"]")
        List<String> categories,

        @Schema(description = "Indica si el libro ya se encuentra previamente registrado en el catálogo local", example = "false")
        boolean alreadyExistsInLocalCatalog
) {}
