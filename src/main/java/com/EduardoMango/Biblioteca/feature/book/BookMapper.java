package com.EduardoMango.Biblioteca.feature.book;

import com.EduardoMango.Biblioteca.feature.author.AuthorMapper;
import com.EduardoMango.Biblioteca.feature.book.dto.BookCreateRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookResponse;
import com.EduardoMango.Biblioteca.feature.category.CategoryMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, AuthorMapper.class})
public interface BookMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stockDisponible", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "autores", ignore = true)
    Book toEntity(BookCreateRequest request);

    BookResponse toResponse(Book book);
}

