package com.EduardoMango.Biblioteca.feature.book.service;

import com.EduardoMango.Biblioteca.feature.author.service.AuthorDeletionValidator;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("bookAuthorDeletionValidator")
@Primary
@RequiredArgsConstructor
public class BookAuthorDeletionValidator implements AuthorDeletionValidator {

    private final BookRepository bookRepository;

    @Override
    public boolean hasAssociatedBooks(UUID authorPublicId) {
        return bookRepository.existsByAutores_PublicId(authorPublicId);
    }
}

