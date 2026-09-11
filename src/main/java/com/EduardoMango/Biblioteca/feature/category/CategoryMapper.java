package com.EduardoMango.Biblioteca.feature.category;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryRequest;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    Category toEntity(CategoryRequest request);

    CategoryResponse toResponse(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    void updateEntityFromRequest(CategoryRequest request, @MappingTarget Category category);
}

