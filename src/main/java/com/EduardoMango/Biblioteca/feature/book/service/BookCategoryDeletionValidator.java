package com.EduardoMango.Biblioteca.feature.book.service;

import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.category.service.CategoryDeletionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("bookCategoryDeletionValidator")
@Primary
@RequiredArgsConstructor
public class BookCategoryDeletionValidator implements CategoryDeletionValidator {

    private final BookRepository bookRepository;

    @Override
    public boolean hasAssociatedBooks(UUID categoryPublicId) {
        return bookRepository.existsByCategoria_PublicId(categoryPublicId);
    }
}

