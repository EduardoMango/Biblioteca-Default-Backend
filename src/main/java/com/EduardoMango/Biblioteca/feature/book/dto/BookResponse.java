package com.EduardoMango.Biblioteca.feature.book.dto;

import com.EduardoMango.Biblioteca.feature.author.dto.AuthorResponse;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryResponse;

import java.util.List;
import java.util.UUID;

public record BookResponse(
        UUID publicId,
        String isbn,
        String titulo,
        Integer stockTotal,
        Integer stockDisponible,
        CategoryResponse categoria,
        List<AuthorResponse> autores
) {
}

