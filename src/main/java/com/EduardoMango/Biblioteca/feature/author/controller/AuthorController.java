package com.EduardoMango.Biblioteca.feature.author.controller;

import com.EduardoMango.Biblioteca.feature.author.dto.AuthorRequest;
import com.EduardoMango.Biblioteca.feature.author.dto.AuthorResponse;
import com.EduardoMango.Biblioteca.feature.author.service.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/autores")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    @PostMapping
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<AuthorResponse> createAuthor(@Valid @RequestBody AuthorRequest request) {
        AuthorResponse response = authorService.createAuthor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<List<AuthorResponse>> getAuthors(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String nacionalidad) {
        return ResponseEntity.ok(authorService.getAuthors(q, nacionalidad));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<AuthorResponse> getAuthorByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(authorService.getAuthorByPublicId(publicId));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<AuthorResponse> updateAuthor(
            @PathVariable UUID publicId,
            @Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.ok(authorService.updateAuthor(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> deleteAuthor(@PathVariable UUID publicId) {
        authorService.deleteAuthor(publicId);
        return ResponseEntity.noContent().build();
    }
}

