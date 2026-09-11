package com.EduardoMango.Biblioteca.feature.category.service;

import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.category.Category;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryRequest;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryResponse;
import com.EduardoMango.Biblioteca.feature.category.CategoryMapper;
import com.EduardoMango.Biblioteca.feature.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CategoryDeletionValidator categoryDeletionValidator;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String trimmedName = request.nombre().trim();
        if (categoryRepository.existsByNombreIgnoreCase(trimmedName)) {
            throw new BusinessRuleException("Ya existe una categoría con el nombre especificado");
        }

        Category category = categoryMapper.toEntity(request);
        category.setNombre(trimmedName);
        if (category.getPublicId() == null) {
            category.setPublicId(UUID.randomUUID());
        }

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    public Page<CategoryResponse> getCategories(Pageable pageable) {
        Pageable effectivePageable = (pageable == null || pageable.isUnpaged())
                ? PageRequest.of(0, 15)
                : pageable;
        return categoryRepository.findAll(effectivePageable).map(categoryMapper::toResponse);
    }

    public CategoryResponse getCategoryByPublicId(UUID publicId) {
        return categoryRepository.findByPublicId(publicId)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con publicId: " + publicId));
    }

    @Transactional
    public CategoryResponse updateCategory(UUID publicId, CategoryRequest request) {
        Category category = categoryRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con publicId: " + publicId));

        String trimmedName = request.nombre().trim();
        if (categoryRepository.existsByNombreIgnoreCaseAndPublicIdNot(trimmedName, publicId)) {
            throw new BusinessRuleException("Ya existe una categoría con el nombre especificado");
        }

        categoryMapper.updateEntityFromRequest(request, category);
        category.setNombre(trimmedName);

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(UUID publicId) {
        Category category = categoryRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con publicId: " + publicId));

        if (categoryDeletionValidator.hasAssociatedBooks(publicId)) {
            throw new BusinessRuleException("No se puede eliminar la categoría porque tiene libros asociados");
        }

        categoryRepository.delete(category);
    }
}

