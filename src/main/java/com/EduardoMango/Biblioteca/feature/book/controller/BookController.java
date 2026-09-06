package com.EduardoMango.Biblioteca.feature.book.controller;

import com.EduardoMango.Biblioteca.feature.book.dto.BookCreateRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookResponse;
import com.EduardoMango.Biblioteca.feature.book.dto.BookUpdateRequest;
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
            @RequestParam(required = false) UUID categoriaPublicId,
            @RequestParam(required = false) UUID autorPublicId,
            @RequestParam(required = false) Boolean soloDisponibles,
            @PageableDefault(page = 0, size = 10, sort = "titulo", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(bookService.searchBooks(titulo, categoriaPublicId, autorPublicId, soloDisponibles, pageable));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> getBookByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(bookService.getBookByPublicId(publicId));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable UUID publicId,
            @Valid @RequestBody BookUpdateRequest request) {
        return ResponseEntity.ok(bookService.updateBook(publicId, request));
    }

    @PatchMapping("/{publicId}/stock")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> adjustStock(
            @PathVariable UUID publicId,
            @Valid @RequestBody com.EduardoMango.Biblioteca.feature.book.dto.StockAdjustmentRequest request) {
        return ResponseEntity.ok(bookService.adjustStock(publicId, request));
    }
}
