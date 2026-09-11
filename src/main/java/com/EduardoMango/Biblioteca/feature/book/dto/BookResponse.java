package com.EduardoMango.Biblioteca.feature.book.dto;

import com.EduardoMango.Biblioteca.feature.author.dto.AuthorResponse;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryResponse;

import java.util.List;

public record BookResponse(
        String isbn,
        String titulo,
        String urlPortada,
        Integer stockTotal,
        Integer stockDisponible,
        CategoryResponse categoria,
        List<AuthorResponse> autores
) {
    public BookResponse(String isbn, String titulo, Integer stockTotal, Integer stockDisponible, CategoryResponse categoria, List<AuthorResponse> autores) {
        this(isbn, titulo, null, stockTotal, stockDisponible, categoria, autores);
    }
}

