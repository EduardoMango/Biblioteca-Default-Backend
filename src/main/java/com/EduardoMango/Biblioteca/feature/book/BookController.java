package com.EduardoMango.Biblioteca.feature.book;

import com.EduardoMango.Biblioteca.feature.book.dto.BookCoverUpdateRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookCreateRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookPatchRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookResponse;
import com.EduardoMango.Biblioteca.feature.book.dto.BookUpdateRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.StockAdjustmentRequest;
import com.EduardoMango.Biblioteca.feature.book.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.UUID;

@RestController
@RequestMapping("/api/libros")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookCreateRequest request) {
        BookResponse response = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<Page<BookResponse>> searchBooks(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) UUID categoriaPublicId,
            @RequestParam(required = false) UUID autorPublicId,
            @RequestParam(required = false) Boolean soloDisponibles,
            @PageableDefault(page = 0, size = 10, sort = "titulo", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(bookService.searchBooks(titulo, isbn, categoriaPublicId, autorPublicId, soloDisponibles, pageable));
    }

    @GetMapping("/{isbn}")
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> getBookByIsbn(@PathVariable String isbn) {
        return ResponseEntity.ok(bookService.getBookByIsbn(isbn));
    }

    @PutMapping("/{isbn}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable String isbn,
            @Valid @RequestBody BookUpdateRequest request) {
        return ResponseEntity.ok(bookService.updateBook(isbn, request));
    }

    @PatchMapping("/{isbn}/stock")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> adjustStock(
            @PathVariable String isbn,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(bookService.adjustStock(isbn, request));
    }

    @PatchMapping("/{isbn}/portada")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> updateCover(
            @PathVariable String isbn,
            @Valid @RequestBody BookCoverUpdateRequest request) {
        return ResponseEntity.ok(bookService.updateCover(isbn, request.urlPortada()));
    }

    @PatchMapping("/{isbn}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> patchBook(
            @PathVariable String isbn,
            @Valid @RequestBody BookPatchRequest request) {
        return ResponseEntity.ok(bookService.patchBook(isbn, request));
    }
}
