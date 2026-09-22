package com.EduardoMango.Biblioteca.feature.book.service;

import com.EduardoMango.Biblioteca.domain.book.dto.ExternalBookDto;
import com.EduardoMango.Biblioteca.domain.book.port.out.ExternalBookSearchPort;
import com.EduardoMango.Biblioteca.exception.ConflictException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.author.Author;
import com.EduardoMango.Biblioteca.feature.author.AuthorRepository;
import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.book.BookMapper;
import com.EduardoMango.Biblioteca.feature.book.dto.BookResponse;
import com.EduardoMango.Biblioteca.feature.book.dto.ExternalBookPreviewDto;
import com.EduardoMango.Biblioteca.feature.book.dto.ImportBookRequest;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.category.Category;
import com.EduardoMango.Biblioteca.feature.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookImportService {

    private final ExternalBookSearchPort externalBookSearchPort;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final BookMapper bookMapper;

    public Mono<BookResponse> importBook(ImportBookRequest request) {
        String trimmedIsbn = request.isbn().trim();
        if (bookRepository.existsByIsbn(trimmedIsbn)) {
            return Mono.error(new ConflictException("El ISBN ya pertenece al catálogo local"));
        }

        return externalBookSearchPort.findByIsbn(trimmedIsbn)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "No se pudo importar porque el libro no fue encontrado en el proveedor externo")))
                .flatMap(externalDto -> Mono.fromCallable(() -> persistBook(externalDto, request.totalCopies(), trimmedIsbn))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(bookMapper::toResponse));
    }

    public Mono<ExternalBookPreviewDto> previewExternalBook(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Mono.error(new ResourceNotFoundException("El ISBN ingresado no es válido"));
        }
        String trimmedIsbn = isbn.trim();

        return externalBookSearchPort.findByIsbn(trimmedIsbn)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Libro no encontrado en el proveedor externo con ISBN: " + trimmedIsbn)))
                .flatMap(dto -> Mono.fromCallable(() -> {
                    boolean alreadyExists = bookRepository.existsByIsbn(trimmedIsbn)
                            || (dto.isbn() != null && bookRepository.existsByIsbn(dto.isbn().trim()));
                    return new ExternalBookPreviewDto(
                            dto.isbn() != null ? dto.isbn() : trimmedIsbn,
                            dto.title(),
                            dto.authors(),
                            dto.publisher(),
                            dto.description(),
                            dto.coverUrl(),
                            dto.categories(),
                            alreadyExists
                    );
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @Transactional
    public Book persistBook(ExternalBookDto dto, Integer totalCopies, String requestedIsbn) {
        String finalIsbn = (dto.isbn() != null && !dto.isbn().isBlank()) ? dto.isbn().trim() : requestedIsbn;

        // Double check in transaction
        if (bookRepository.existsByIsbn(finalIsbn)) {
            throw new ConflictException("El ISBN ya pertenece al catálogo local");
        }

        String categoryName = (dto.categories() != null && !dto.categories().isEmpty())
                ? dto.categories().get(0).trim()
                : "General";

        Category category = categoryRepository.findByNombreIgnoreCase(categoryName)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .nombre(categoryName)
                        .descripcion("Categoría importada desde Google Books")
                        .publicId(UUID.randomUUID())
                        .build()));

        List<Author> authors = new ArrayList<>();
        if (dto.authors() != null && !dto.authors().isEmpty()) {
            for (String rawAuthor : dto.authors()) {
                String trimmed = rawAuthor.trim();
                String nombre;
                String apellido;
                int lastSpace = trimmed.lastIndexOf(' ');
                if (lastSpace > 0) {
                    nombre = trimmed.substring(0, lastSpace).trim();
                    apellido = trimmed.substring(lastSpace + 1).trim();
                } else {
                    nombre = trimmed;
                    apellido = trimmed;
                }

                Author author = authorRepository.findByNombreIgnoreCaseAndApellidoIgnoreCase(nombre, apellido)
                        .orElseGet(() -> authorRepository.save(Author.builder()
                                .nombre(nombre)
                                .apellido(apellido)
                                .publicId(UUID.randomUUID())
                                .build()));
                authors.add(author);
            }
        } else {
            Author defaultAuthor = authorRepository.findByNombreIgnoreCaseAndApellidoIgnoreCase("Autor", "Desconocido")
                    .orElseGet(() -> authorRepository.save(Author.builder()
                            .nombre("Autor")
                            .apellido("Desconocido")
                            .publicId(UUID.randomUUID())
                            .build()));
            authors.add(defaultAuthor);
        }

        Book book = Book.builder()
                .isbn(finalIsbn)
                .titulo(dto.title() != null ? dto.title().trim() : "Sin título")
                .urlPortada(dto.coverUrl())
                .descripcion(dto.description())
                .editorial(dto.publisher())
                .stockTotal(totalCopies)
                .stockDisponible(totalCopies)
                .categoria(category)
                .autores(authors)
                .publicId(UUID.randomUUID())
                .build();

        return bookRepository.save(book);
    }
}

