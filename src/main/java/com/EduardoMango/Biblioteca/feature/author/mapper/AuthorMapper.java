package com.EduardoMango.Biblioteca.feature.author.mapper;

import com.EduardoMango.Biblioteca.feature.author.domain.Author;
import com.EduardoMango.Biblioteca.feature.author.dto.AuthorRequest;
import com.EduardoMango.Biblioteca.feature.author.dto.AuthorResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    Author toEntity(AuthorRequest request);

    AuthorResponse toResponse(Author author);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    void updateEntityFromRequest(AuthorRequest request, @MappingTarget Author author);
}

