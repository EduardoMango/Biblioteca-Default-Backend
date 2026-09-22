package com.EduardoMango.Biblioteca.feature.book.dto;

import com.EduardoMango.Biblioteca.feature.author.dto.AuthorResponse;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Representación detallada de un libro en el catálogo")
public record BookResponse(
        @Schema(description = "Identificador público inmutable del libro", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID publicId,

        @Schema(description = "Código ISBN único", example = "9780307474728")
        String isbn,

        @Schema(description = "Título de la obra", example = "Cien años de soledad")
        String titulo,

        @Schema(description = "URL de la portada", example = "https://images.example.com/portadas/9780307474728.jpg")
        String urlPortada,

        @Schema(description = "Total de ejemplares físicos registrados", example = "5")
        Integer stockTotal,

        @Schema(description = "Ejemplares físicos disponibles para nuevos préstamos", example = "3")
        Integer stockDisponible,

        @Schema(description = "Categoría a la que pertenece")
        CategoryResponse categoria,

        @Schema(description = "Lista de autores de la obra")
        List<AuthorResponse> autores,

        @Schema(description = "Sinopsis o descripción del libro", example = "Obra cumbre del realismo mágico...")
        String descripcion,

        @Schema(description = "Casa editorial", example = "Editorial Sudamericana")
        String editorial
) {
    public BookResponse(String isbn, String titulo, Integer stockTotal, Integer stockDisponible, CategoryResponse categoria, List<AuthorResponse> autores) {
        this(null, isbn, titulo, null, stockTotal, stockDisponible, categoria, autores, null, null);
    }

    public BookResponse(String isbn, String titulo, String urlPortada, Integer stockTotal, Integer stockDisponible, CategoryResponse categoria, List<AuthorResponse> autores) {
        this(null, isbn, titulo, urlPortada, stockTotal, stockDisponible, categoria, autores, null, null);
    }
}
