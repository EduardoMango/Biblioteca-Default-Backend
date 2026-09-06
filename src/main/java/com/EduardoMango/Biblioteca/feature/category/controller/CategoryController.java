package com.EduardoMango.Biblioteca.feature.category.controller;

import com.EduardoMango.Biblioteca.feature.category.dto.CategoryRequest;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryResponse;
import com.EduardoMango.Biblioteca.feature.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<Page<CategoryResponse>> getCategories(Pageable pageable) {
        return ResponseEntity.ok(categoryService.getCategories(pageable));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<CategoryResponse> getCategoryByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(categoryService.getCategoryByPublicId(publicId));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID publicId,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID publicId) {
        categoryService.deleteCategory(publicId);
        return ResponseEntity.noContent().build();
    }
}

