package com.EduardoMango.Biblioteca.domain.book.service;

import com.EduardoMango.Biblioteca.domain.book.dto.ExternalBookDto;
import com.EduardoMango.Biblioteca.domain.book.port.out.ExternalBookSearchPort;
import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.book.BookMapper;
import com.EduardoMango.Biblioteca.feature.book.dto.BookResponse;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookSyncService {

    private final ExternalBookSearchPort externalBookSearchPort;
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final TransactionTemplate transactionTemplate;

    public Mono<BookResponse> syncBook(String identifier, boolean force) {
        if (identifier == null || identifier.isBlank()) {
            return Mono.error(new ResourceNotFoundException("Identificador de libro inválido"));
        }

        Book book;
        try {
            book = findBookByIdentifier(identifier.trim());
        } catch (Exception e) {
            return Mono.error(e);
        }

        if (book.getIsbn() == null || book.getIsbn().isBlank()) {
            return Mono.error(new BusinessRuleException("No se puede sincronizar un libro sin ISBN asociado"));
        }

        String isbn = book.getIsbn().trim();

        return externalBookSearchPort.findByIsbn(isbn)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "No se encontró información en Google Books para el ISBN: " + isbn)))
                .flatMap(externalDto -> Mono.fromCallable(() -> transactionTemplate.execute(status ->
                                applySyncAndSave(book.getId(), externalDto, force)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Book findBookByIdentifier(String identifier) {
        try {
            UUID publicId = UUID.fromString(identifier);
            return bookRepository.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con publicId: " + identifier));
        } catch (IllegalArgumentException e) {
            return bookRepository.findByIsbn(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con identificador: " + identifier));
        }
    }

    public BookResponse applySyncAndSave(Long bookId, ExternalBookDto externalDto, boolean force) {
        Book target = bookRepository.findWithDetailsById(bookId)
                .orElseGet(() -> bookRepository.findById(bookId)
                        .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + bookId)));

        if (force) {
            if (externalDto.title() != null && !externalDto.title().isBlank()) {
                target.setTitulo(externalDto.title().trim());
            }
            target.setEditorial(externalDto.publisher());
            target.setDescripcion(externalDto.description());
            target.setUrlPortada(externalDto.coverUrl());
        } else {
            if ((target.getTitulo() == null || target.getTitulo().isBlank()) && externalDto.title() != null) {
                target.setTitulo(externalDto.title().trim());
            }
            if ((target.getEditorial() == null || target.getEditorial().isBlank()) && externalDto.publisher() != null) {
                target.setEditorial(externalDto.publisher().trim());
            }
            if ((target.getDescripcion() == null || target.getDescripcion().isBlank()) && externalDto.description() != null) {
                target.setDescripcion(externalDto.description().trim());
            }
            if ((target.getUrlPortada() == null || target.getUrlPortada().isBlank()) && externalDto.coverUrl() != null) {
                target.setUrlPortada(externalDto.coverUrl().trim());
            }
        }

        Book saved = bookRepository.save(target);
        return bookMapper.toResponse(saved);
    }
}

