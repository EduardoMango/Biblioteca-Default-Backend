package com.EduardoMango.Biblioteca.feature.book.service;

import com.EduardoMango.Biblioteca.domain.book.dto.ExternalBookDto;
import com.EduardoMango.Biblioteca.domain.book.port.out.ExternalBookSearchPort;
import com.EduardoMango.Biblioteca.exception.ConflictException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.author.Author;
import com.EduardoMango.Biblioteca.feature.author.AuthorRepository;
import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.book.BookMapper;
import com.EduardoMango.Biblioteca.feature.book.dto.BookResponse;
import com.EduardoMango.Biblioteca.feature.book.dto.ImportBookRequest;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.category.Category;
import com.EduardoMango.Biblioteca.feature.category.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookImportServiceTest {

    @Mock
    private ExternalBookSearchPort externalBookSearchPort;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookImportService bookImportService;

    @Test
    @DisplayName("Escenario 1: Importación exitosa de libro crea entidades y persiste stock")
    void testImportBook_Success() {
        String isbn = "9780134685991";
        ImportBookRequest request = new ImportBookRequest(isbn, 5);

        ExternalBookDto externalDto = new ExternalBookDto(
                isbn,
                "Effective Java",
                List.of("Joshua Bloch"),
                "Addison-Wesley",
                "Best practices",
                "https://example.com/cover.jpg",
                List.of("Programming"),
                412,
                "2018"
        );

        when(bookRepository.existsByIsbn(isbn)).thenReturn(false);
        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));
        when(categoryRepository.findByNombreIgnoreCase("Programming")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(authorRepository.findByNombreIgnoreCaseAndApellidoIgnoreCase("Joshua", "Bloch")).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse expectedResponse = new BookResponse(
                UUID.randomUUID(),
                isbn,
                "Effective Java",
                "https://example.com/cover.jpg",
                5,
                5,
                null,
                null,
                "Best practices",
                "Addison-Wesley"
        );
        when(bookMapper.toResponse(any(Book.class))).thenReturn(expectedResponse);

        StepVerifier.create(bookImportService.importBook(request))
                .assertNext(response -> {
                    assertEquals(isbn, response.isbn());
                    assertEquals("Effective Java", response.titulo());
                    assertEquals(5, response.stockTotal());
                    assertEquals(5, response.stockDisponible());
                })
                .verifyComplete();

        verify(bookRepository, times(2)).existsByIsbn(isbn);
        verify(externalBookSearchPort).findByIsbn(isbn);
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("Escenario 2: Intento de importación con ISBN duplicado cancela llamada externa y lanza ConflictException")
    void testImportBook_DuplicateIsbn_ThrowsConflictException() {
        String isbn = "9780134685991";
        ImportBookRequest request = new ImportBookRequest(isbn, 5);

        when(bookRepository.existsByIsbn(isbn)).thenReturn(true);

        StepVerifier.create(bookImportService.importBook(request))
                .expectErrorMatches(throwable -> throwable instanceof ConflictException &&
                        throwable.getMessage().contains("El ISBN ya pertenece al catálogo local"))
                .verify();

        verifyNoInteractions(externalBookSearchPort);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Escenario 4: Importación con ISBN no encontrado en Google Books lanza ResourceNotFoundException")
    void testImportBook_NotFoundInExternal_ThrowsResourceNotFoundException() {
        String isbn = "0000000000000";
        ImportBookRequest request = new ImportBookRequest(isbn, 5);

        when(bookRepository.existsByIsbn(isbn)).thenReturn(false);
        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.empty());

        StepVerifier.create(bookImportService.importBook(request))
                .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException &&
                        throwable.getMessage().contains("no fue encontrado en el proveedor externo"))
                .verify();

        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Escenario 3: Previsualización de libro externo exitosa")
    void testPreviewExternalBook_Success() {
        String isbn = "9780134685991";
        ExternalBookDto externalDto = new ExternalBookDto(
                isbn,
                "Effective Java",
                List.of("Joshua Bloch"),
                "Addison-Wesley",
                "Best practices",
                "https://example.com/cover.jpg",
                List.of("Programming"),
                412,
                "2018"
        );

        when(bookRepository.existsByIsbn(isbn)).thenReturn(false);
        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));

        StepVerifier.create(bookImportService.previewExternalBook(isbn))
                .assertNext(preview -> {
                    assertEquals(isbn, preview.isbn());
                    assertEquals("Effective Java", preview.title());
                    assertEquals("https://example.com/cover.jpg", preview.coverUrl());
                    assertEquals(false, preview.alreadyExistsInLocalCatalog());
                })
                .verifyComplete();

        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Previsualización marca alreadyExistsInLocalCatalog como true si el libro ya está registrado")
    void testPreviewExternalBook_AlreadyInCatalog() {
        String isbn = "9780134685991";
        ExternalBookDto externalDto = new ExternalBookDto(
                isbn,
                "Effective Java",
                List.of("Joshua Bloch"),
                "Addison-Wesley",
                "Best practices",
                "https://example.com/cover.jpg",
                List.of("Programming"),
                412,
                "2018"
        );

        when(bookRepository.existsByIsbn(isbn)).thenReturn(true);
        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));

        StepVerifier.create(bookImportService.previewExternalBook(isbn))
                .assertNext(preview -> {
                    assertTrue(preview.alreadyExistsInLocalCatalog());
                })
                .verifyComplete();
    }
}

