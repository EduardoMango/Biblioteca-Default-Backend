package com.EduardoMango.Biblioteca.feature.category;

import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryRequest;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryResponse;
import com.EduardoMango.Biblioteca.feature.category.service.CategoryDeletionValidator;
import com.EduardoMango.Biblioteca.feature.category.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CategoryDeletionValidator categoryDeletionValidator;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("Dado un request válido, crea la categoría exitosamente")
    void createCategory_ValidRequest_Success() {
        CategoryRequest request = new CategoryRequest("Ciencia Ficción", "Novelas y relatos");
        Category category = Category.builder().nombre("Ciencia Ficción").descripcion("Novelas y relatos").build();
        Category savedCategory = Category.builder().id(1L).publicId(UUID.randomUUID()).nombre("Ciencia Ficción").descripcion("Novelas y relatos").build();
        CategoryResponse response = new CategoryResponse(savedCategory.getPublicId(), "Ciencia Ficción", "Novelas y relatos");

        when(categoryRepository.existsByNombreIgnoreCase("Ciencia Ficción")).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(categoryMapper.toResponse(savedCategory)).thenReturn(response);

        CategoryResponse result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals("Ciencia Ficción", result.nombre());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Dado un nombre duplicado, lanza BusinessRuleException")
    void createCategory_DuplicateName_ThrowsBusinessRuleException() {
        CategoryRequest request = new CategoryRequest("programación", "Libros tech");
        when(categoryRepository.existsByNombreIgnoreCase("programación")).thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                categoryService.createCategory(request));

        assertEquals("Ya existe una categoría con el nombre especificado", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dado un publicId inexistente, lanzar ResourceNotFoundException al consultar")
    void getCategoryByPublicId_NotFound_ThrowsResourceNotFoundException() {
        UUID publicId = UUID.randomUUID();
        when(categoryRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                categoryService.getCategoryByPublicId(publicId));
    }

    @Test
    @DisplayName("Dado un publicId existente, retornar la categoría mapeada")
    void getCategoryByPublicId_Found_ReturnsCategory() {
        UUID publicId = UUID.randomUUID();
        Category category = Category.builder().id(1L).publicId(publicId).nombre("Historia").build();
        CategoryResponse response = new CategoryResponse(publicId, "Historia", null);

        when(categoryRepository.findByPublicId(publicId)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        CategoryResponse result = categoryService.getCategoryByPublicId(publicId);

        assertNotNull(result);
        assertEquals(publicId, result.publicId());
        assertEquals("Historia", result.nombre());
    }

    @Test
    @DisplayName("Dado un intento de actualizar con nombre duplicado de otra categoría, lanzar BusinessRuleException")
    void updateCategory_DuplicateNameOtherCategory_ThrowsBusinessRuleException() {
        UUID publicId = UUID.randomUUID();
        Category existing = Category.builder().id(1L).publicId(publicId).nombre("Fantasía").build();
        CategoryRequest request = new CategoryRequest("Terror", "Descripción");

        when(categoryRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNombreIgnoreCaseAndPublicIdNot("Terror", publicId)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () ->
                categoryService.updateCategory(publicId, request));

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dado un intento de eliminar categoría con libros vinculados, rechazar y lanzar BusinessRuleException")
    void deleteCategory_WithAssociatedBooks_ThrowsBusinessRuleException() {
        UUID publicId = UUID.randomUUID();
        Category category = Category.builder().id(1L).publicId(publicId).nombre("Drama").build();

        when(categoryRepository.findByPublicId(publicId)).thenReturn(Optional.of(category));
        when(categoryDeletionValidator.hasAssociatedBooks(publicId)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                categoryService.deleteCategory(publicId));

        assertEquals("No se puede eliminar la categoría porque tiene libros asociados", ex.getMessage());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Dado un intento de eliminar categoría sin libros, eliminar exitosamente")
    void deleteCategory_WithoutBooks_Success() {
        UUID publicId = UUID.randomUUID();
        Category category = Category.builder().id(1L).publicId(publicId).nombre("Drama").build();

        when(categoryRepository.findByPublicId(publicId)).thenReturn(Optional.of(category));
        when(categoryDeletionValidator.hasAssociatedBooks(publicId)).thenReturn(false);

        categoryService.deleteCategory(publicId);

        verify(categoryRepository).delete(category);
    }
}

