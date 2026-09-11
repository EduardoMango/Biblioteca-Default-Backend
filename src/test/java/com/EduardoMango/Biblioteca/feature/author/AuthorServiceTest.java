package com.EduardoMango.Biblioteca.feature.author;

import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.author.dto.AuthorRequest;
import com.EduardoMango.Biblioteca.feature.author.dto.AuthorResponse;
import com.EduardoMango.Biblioteca.feature.author.service.AuthorDeletionValidator;
import com.EduardoMango.Biblioteca.feature.author.service.AuthorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private AuthorMapper authorMapper;

    @Mock
    private AuthorDeletionValidator authorDeletionValidator;

    @InjectMocks
    private AuthorService authorService;

    @Test
    @DisplayName("Dado un request válido, crea el autor exitosamente")
    void createAuthor_Success() {
        AuthorRequest request = new AuthorRequest("Gabriel", "García Márquez", "Colombiana", LocalDate.of(1927, 3, 6));
        Author author = Author.builder().nombre("Gabriel").apellido("García Márquez").nacionalidad("Colombiana").build();
        Author saved = Author.builder().id(1L).publicId(UUID.randomUUID()).nombre("Gabriel").apellido("García Márquez").nacionalidad("Colombiana").build();
        AuthorResponse response = new AuthorResponse(saved.getPublicId(), "Gabriel", "García Márquez", "Colombiana", LocalDate.of(1927, 3, 6));

        when(authorMapper.toEntity(request)).thenReturn(author);
        when(authorRepository.save(any(Author.class))).thenReturn(saved);
        when(authorMapper.toResponse(saved)).thenReturn(response);

        AuthorResponse result = authorService.createAuthor(request);

        assertNotNull(result);
        assertEquals("Gabriel", result.nombre());
        assertEquals("García Márquez", result.apellido());
        verify(authorRepository).save(any(Author.class));
    }

    @Test
    @DisplayName("Dado un publicId inexistente, lanza ResourceNotFoundException")
    void getAuthorByPublicId_NotFound_ThrowsResourceNotFoundException() {
        UUID publicId = UUID.randomUUID();
        when(authorRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                authorService.getAuthorByPublicId(publicId));
    }

    @Test
    @DisplayName("Dado un filtro de búsqueda, retorna autores coincidentes")
    void getAuthors_WithFilter_ReturnsMatchingAuthors() {
        Author author1 = Author.builder().id(1L).publicId(UUID.randomUUID()).nombre("Robert").apellido("Martin").build();
        AuthorResponse resp1 = new AuthorResponse(author1.getPublicId(), "Robert", "Martin", null, null);

        when(authorRepository.searchAuthors("Martin", null)).thenReturn(List.of(author1));
        when(authorMapper.toResponse(author1)).thenReturn(resp1);

        List<AuthorResponse> results = authorService.getAuthors("Martin", null);

        assertEquals(1, results.size());
        assertEquals("Martin", results.get(0).apellido());
    }

    @Test
    @DisplayName("Dado un intento de eliminar autor con libros vinculados, rechazar y lanzar BusinessRuleException")
    void deleteAuthor_WithAssociatedBooks_ThrowsBusinessRuleException() {
        UUID publicId = UUID.randomUUID();
        Author author = Author.builder().id(1L).publicId(publicId).nombre("Jorge").apellido("Borges").build();

        when(authorRepository.findByPublicId(publicId)).thenReturn(Optional.of(author));
        when(authorDeletionValidator.hasAssociatedBooks(publicId)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                authorService.deleteAuthor(publicId));

        assertEquals("El autor no puede ser eliminado por tener obras asociadas", ex.getMessage());
        verify(authorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Dado un intento de eliminar autor sin libros, eliminar exitosamente")
    void deleteAuthor_WithoutBooks_Success() {
        UUID publicId = UUID.randomUUID();
        Author author = Author.builder().id(1L).publicId(publicId).nombre("Jorge").apellido("Borges").build();

        when(authorRepository.findByPublicId(publicId)).thenReturn(Optional.of(author));
        when(authorDeletionValidator.hasAssociatedBooks(publicId)).thenReturn(false);

        authorService.deleteAuthor(publicId);

        verify(authorRepository).delete(author);
    }
}

