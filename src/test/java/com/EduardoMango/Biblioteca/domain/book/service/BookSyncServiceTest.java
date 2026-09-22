package com.EduardoMango.Biblioteca.domain.book.service;

import com.EduardoMango.Biblioteca.domain.book.dto.ExternalBookDto;
import com.EduardoMango.Biblioteca.domain.book.port.out.ExternalBookSearchPort;
import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.book.BookMapper;
import com.EduardoMango.Biblioteca.feature.book.dto.BookResponse;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookSyncServiceTest {

    @Mock
    private ExternalBookSearchPort externalBookSearchPort;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private BookSyncService bookSyncService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    @DisplayName("Escenario 1: Enriquecimiento exitoso de campos vacíos (force = false)")
    void testSyncBook_NonDestructiveUpdate() {
        UUID publicId = UUID.randomUUID();
        String isbn = "9780134685991";

        Book existingBook = Book.builder()
                .id(1L)
                .publicId(publicId)
                .isbn(isbn)
                .titulo("Título Local")
                .urlPortada(null)
                .descripcion(null)
                .editorial(null)
                .stockTotal(5)
                .stockDisponible(2)
                .build();

        ExternalBookDto externalDto = new ExternalBookDto(
                isbn,
                "Título Google Books",
                List.of("Joshua Bloch"),
                "Addison-Wesley",
                "Descripción Google Books",
                "https://example.com/cover.jpg",
                List.of("Tech")
        );

        when(bookRepository.findByPublicId(publicId)).thenReturn(Optional.of(existingBook));
        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse expectedResponse = new BookResponse(
                publicId,
                isbn,
                "Título Local",
                "https://example.com/cover.jpg",
                5,
                2,
                null,
                null,
                "Descripción Google Books",
                "Addison-Wesley"
        );
        when(bookMapper.toResponse(any(Book.class))).thenReturn(expectedResponse);

        StepVerifier.create(bookSyncService.syncBook(publicId.toString(), false))
                .assertNext(res -> {
                    assertEquals("Título Local", res.titulo());
                    assertEquals("https://example.com/cover.jpg", res.urlPortada());
                    assertEquals("Descripción Google Books", res.descripcion());
                    assertEquals("Addison-Wesley", res.editorial());
                    assertEquals(5, res.stockTotal());
                    assertEquals(2, res.stockDisponible());
                })
                .verifyComplete();

        verify(bookRepository).save(argThat(b ->
                "Título Local".equals(b.getTitulo()) &&
                "https://example.com/cover.jpg".equals(b.getUrlPortada()) &&
                "Descripción Google Books".equals(b.getDescripcion()) &&
                "Addison-Wesley".equals(b.getEditorial()) &&
                b.getStockTotal() == 5 &&
                b.getStockDisponible() == 2
        ));
    }

    @Test
    @DisplayName("Escenario 2: Sobrescritura forzada de metadatos (force = true)")
    void testSyncBook_ForceOverwrite() {
        UUID publicId = UUID.randomUUID();
        String isbn = "9780134685991";

        Book existingBook = Book.builder()
                .id(2L)
                .publicId(publicId)
                .isbn(isbn)
                .titulo("Título Antiguo")
                .urlPortada("https://example.com/old-cover.jpg")
                .descripcion("Descripción Antigua")
                .editorial("Editorial Antigua")
                .stockTotal(10)
                .stockDisponible(4)
                .build();

        ExternalBookDto externalDto = new ExternalBookDto(
                isbn,
                "Título Nuevo",
                List.of("Joshua Bloch"),
                "Editorial Nueva",
                "Descripción Nueva",
                "https://example.com/new-cover.jpg",
                List.of("Tech")
        );

        when(bookRepository.findByPublicId(publicId)).thenReturn(Optional.of(existingBook));
        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(existingBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse expectedResponse = new BookResponse(
                publicId,
                isbn,
                "Título Nuevo",
                "https://example.com/new-cover.jpg",
                10,
                4,
                null,
                null,
                "Descripción Nueva",
                "Editorial Nueva"
        );
        when(bookMapper.toResponse(any(Book.class))).thenReturn(expectedResponse);

        StepVerifier.create(bookSyncService.syncBook(publicId.toString(), true))
                .assertNext(res -> {
                    assertEquals("Título Nuevo", res.titulo());
                    assertEquals("https://example.com/new-cover.jpg", res.urlPortada());
                    assertEquals("Descripción Nueva", res.descripcion());
                    assertEquals("Editorial Nueva", res.editorial());
                    assertEquals(10, res.stockTotal());
                    assertEquals(4, res.stockDisponible());
                })
                .verifyComplete();

        verify(bookRepository).save(argThat(b ->
                "Título Nuevo".equals(b.getTitulo()) &&
                "https://example.com/new-cover.jpg".equals(b.getUrlPortada()) &&
                "Descripción Nueva".equals(b.getDescripcion()) &&
                "Editorial Nueva".equals(b.getEditorial()) &&
                b.getStockTotal() == 10 &&
                b.getStockDisponible() == 4
        ));
    }

    @Test
    @DisplayName("Escenario 3: Intento de sincronización de un libro sin ISBN registrado lanza BusinessRuleException")
    void testSyncBook_WithoutIsbn_ThrowsBusinessRuleException() {
        UUID publicId = UUID.randomUUID();

        Book bookWithoutIsbn = Book.builder()
                .id(3L)
                .publicId(publicId)
                .isbn(null)
                .titulo("Sin ISBN")
                .stockTotal(3)
                .stockDisponible(3)
                .build();

        when(bookRepository.findByPublicId(publicId)).thenReturn(Optional.of(bookWithoutIsbn));

        StepVerifier.create(bookSyncService.syncBook(publicId.toString(), false))
                .expectErrorMatches(throwable -> throwable instanceof BusinessRuleException &&
                        throwable.getMessage().contains("No se puede sincronizar un libro sin ISBN asociado"))
                .verify();

        verifyNoInteractions(externalBookSearchPort);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Sincronización con ISBN no encontrado en Google Books lanza ResourceNotFoundException")
    void testSyncBook_NotFoundInExternal_ThrowsResourceNotFoundException() {
        UUID publicId = UUID.randomUUID();
        String isbn = "9780134685991";

        Book book = Book.builder()
                .id(4L)
                .publicId(publicId)
                .isbn(isbn)
                .titulo("Libro Test")
                .stockTotal(1)
                .stockDisponible(1)
                .build();

        when(bookRepository.findByPublicId(publicId)).thenReturn(Optional.of(book));
        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.empty());

        StepVerifier.create(bookSyncService.syncBook(publicId.toString(), false))
                .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException &&
                        throwable.getMessage().contains("Google Books"))
                .verify();

        verify(bookRepository, never()).save(any(Book.class));
    }
}
