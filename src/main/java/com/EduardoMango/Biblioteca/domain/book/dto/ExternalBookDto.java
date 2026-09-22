package com.EduardoMango.Biblioteca.domain.book.dto;

import java.util.List;

public record ExternalBookDto(
        String isbn,
        String title,
        List<String> authors,
        String publisher,
        String description,
        String coverUrl,
        List<String> categories,
        Integer pageCount,
        String publishedDate
) {
    public ExternalBookDto(String isbn, String title, List<String> authors, String publisher, String description, String coverUrl, List<String> categories) {
        this(isbn, title, authors, publisher, description, coverUrl, categories, null, null);
    }
}

