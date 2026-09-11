package com.EduardoMango.Biblioteca.feature.book;

import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.author.Author;
import com.EduardoMango.Biblioteca.feature.author.AuthorRepository;
import com.EduardoMango.Biblioteca.feature.book.dto.BookCreateRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookPatchRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookResponse;
import com.EduardoMango.Biblioteca.feature.book.dto.BookUpdateRequest;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.book.service.BookService;
import com.EduardoMango.Biblioteca.feature.category.Category;
import com.EduardoMango.Biblioteca.feature.category.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookService bookService;

    @Test
    @DisplayName("Dado un request válido, crea el libro y fija stockDisponible = stockTotal")
    void createBook_Success() {
        UUID catId = UUID.randomUUID();
        UUID aut1 = UUID.randomUUID();
        UUID aut2 = UUID.randomUUID();

        BookCreateRequest request = new BookCreateRequest("978-0132350884", "Clean Code", 5, catId, List.of(aut1, aut2));
        Category category = Category.builder().id(1L).publicId(catId).nombre("Programación").build();
        Author author1 = Author.builder().id(1L).publicId(aut1).nombre("Robert").apellido("Martin").build();
        Author author2 = Author.builder().id(2L).publicId(aut2).nombre("Dean").apellido("Wampler").build();

        Book book = Book.builder().build();
        Book savedBook = Book.builder()
                .id(1L)
                .isbn("978-0132350884")
                .titulo("Clean Code")
                .stockTotal(5)
                .stockDisponible(5)
                .categoria(category)
                .autores(List.of(author1, author2))
                .build();
        BookResponse response = new BookResponse("978-0132350884", "Clean Code", 5, 5, null, null);

        when(bookRepository.existsByIsbn("978-0132350884")).thenReturn(false);
        when(categoryRepository.findByPublicId(catId)).thenReturn(Optional.of(category));
        when(authorRepository.findByPublicId(aut1)).thenReturn(Optional.of(author1));
        when(authorRepository.findByPublicId(aut2)).thenReturn(Optional.of(author2));
        when(bookMapper.toEntity(request)).thenReturn(book);
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);
        when(bookMapper.toResponse(savedBook)).thenReturn(response);

        BookResponse result = bookService.createBook(request);

        assertNotNull(result);
        assertEquals("Clean Code", result.titulo());
        assertEquals(5, result.stockTotal());
        assertEquals(5, result.stockDisponible());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("Dado un ISBN existente, retorna el libro correspondiente")
    void getBookByIsbn_Success() {
        String isbn = "978-0132350884";
        Book book = Book.builder().id(1L).isbn(isbn).titulo("Clean Code").build();
        BookResponse response = new BookResponse(isbn, "Clean Code", 5, 5, null, null);

        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.of(book));
        when(bookMapper.toResponse(book)).thenReturn(response);

        BookResponse result = bookService.getBookByIsbn(isbn);

        assertNotNull(result);
        assertEquals(isbn, result.isbn());
        assertEquals("Clean Code", result.titulo());
    }

    @Test
    @DisplayName("Dado un ISBN no existente, lanza ResourceNotFoundException")
    void getBookByIsbn_NotFound_ThrowsResourceNotFoundException() {
        String isbn = "978-0000000000";
        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookByIsbn(isbn));
    }

    @Test
    @DisplayName("Dado un ISBN duplicado, lanza BusinessRuleException")
    void createBook_DuplicateIsbn_ThrowsBusinessRuleException() {
        BookCreateRequest request = new BookCreateRequest("978-0132350884", "Clean Code", 5, UUID.randomUUID(), List.of(UUID.randomUUID()));
        when(bookRepository.existsByIsbn("978-0132350884")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                bookService.createBook(request));

        assertEquals("Ya existe un libro registrado con el ISBN ingresado", ex.getMessage());
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dado un categoría inexistente, lanza ResourceNotFoundException")
    void createBook_CategoryNotFound_ThrowsResourceNotFoundException() {
        UUID catId = UUID.randomUUID();
        BookCreateRequest request = new BookCreateRequest("978-111", "Título", 3, catId, List.of(UUID.randomUUID()));

        when(bookRepository.existsByIsbn("978-111")).thenReturn(false);
        when(categoryRepository.findByPublicId(catId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                bookService.createBook(request));
    }

    @Test
    @DisplayName("Dado un intento de reducción de stock por debajo de lo prestado en edición, lanza BusinessRuleException")
    void updateBook_ReduceStockBelowBorrowed_ThrowsBusinessRuleException() {
        String isbn = "978-111";
        UUID catId = UUID.randomUUID();
        UUID autId = UUID.randomUUID();

        // stockTotal = 5, stockDisponible = 2 -> prestados = 3
        Book existingBook = Book.builder()
                .id(1L)
                .isbn(isbn)
                .titulo("Libro Original")
                .stockTotal(5)
                .stockDisponible(2)
                .build();

        // Intenta fijar nuevoStockTotal = 2 (< 3)
        BookUpdateRequest request = new BookUpdateRequest("978-111", "Libro Modificado", 2, catId, List.of(autId));
        Category category = Category.builder().id(1L).publicId(catId).build();
        Author author = Author.builder().id(1L).publicId(autId).build();

        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.of(existingBook));
        when(bookRepository.existsByIsbnAndIdNot("978-111", 1L)).thenReturn(false);
        when(categoryRepository.findByPublicId(catId)).thenReturn(Optional.of(category));
        when(authorRepository.findByPublicId(autId)).thenReturn(Optional.of(author));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                bookService.updateBook(isbn, request));

        assertEquals("No se puede reducir el stock total por debajo del número de copias prestadas", ex.getMessage());
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dado un ISBN existente y una nueva URL de portada, actualiza la portada con éxito")
    void updateCover_Success() {
        String isbn = "978-0132350884";
        String nuevaPortada = "https://images.example.com/clean-code.jpg";
        Book book = Book.builder().id(1L).isbn(isbn).titulo("Clean Code").build();
        Book savedBook = Book.builder().id(1L).isbn(isbn).titulo("Clean Code").urlPortada(nuevaPortada).build();
        BookResponse response = new BookResponse(isbn, "Clean Code", nuevaPortada, 5, 5, null, null);

        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(savedBook);
        when(bookMapper.toResponse(savedBook)).thenReturn(response);

        BookResponse result = bookService.updateCover(isbn, nuevaPortada);

        assertNotNull(result);
        assertEquals(nuevaPortada, result.urlPortada());
        verify(bookRepository).save(book);
    }

    @Test
    @DisplayName("Dado un patch request con urlPortada, actualiza la portada con éxito")
    void patchBook_Success() {
        String isbn = "978-0132350884";
        String nuevaPortada = "https://images.example.com/clean-code-patch.jpg";
        BookPatchRequest request = new BookPatchRequest(nuevaPortada);
        Book book = Book.builder().id(1L).isbn(isbn).titulo("Clean Code").build();
        Book savedBook = Book.builder().id(1L).isbn(isbn).titulo("Clean Code").urlPortada(nuevaPortada).build();
        BookResponse response = new BookResponse(isbn, "Clean Code", nuevaPortada, 5, 5, null, null);

        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(savedBook);
        when(bookMapper.toResponse(savedBook)).thenReturn(response);

        BookResponse result = bookService.patchBook(isbn, request);

        assertNotNull(result);
        assertEquals(nuevaPortada, result.urlPortada());
        verify(bookRepository).save(book);
    }

    @Test
    @DisplayName("Dado un ISBN no existente para actualizar portada, lanza ResourceNotFoundException")
    void updateCover_NotFound_ThrowsResourceNotFoundException() {
        String isbn = "978-0000000000";
        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.updateCover(isbn, "https://example.com/cover.jpg"));
        verify(bookRepository, never()).save(any());
    }
}

