package com.EduardoMango.Biblioteca.feature.book.service;

import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.author.domain.Author;
import com.EduardoMango.Biblioteca.feature.author.repository.AuthorRepository;
import com.EduardoMango.Biblioteca.feature.book.domain.Book;
import com.EduardoMango.Biblioteca.feature.book.dto.BookCreateRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookResponse;
import com.EduardoMango.Biblioteca.feature.book.dto.BookUpdateRequest;
import com.EduardoMango.Biblioteca.feature.book.mapper.BookMapper;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.category.domain.Category;
import com.EduardoMango.Biblioteca.feature.category.repository.CategoryRepository;
import com.EduardoMango.Biblioteca.feature.book.repository.BookSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final BookMapper bookMapper;

    @Transactional
    public BookResponse createBook(BookCreateRequest request) {
        String trimmedIsbn = request.isbn().trim();
        if (bookRepository.existsByIsbn(trimmedIsbn)) {
            throw new BusinessRuleException("Ya existe un libro registrado con el ISBN ingresado");
        }

        Category category = categoryRepository.findByPublicId(request.categoriaPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con publicId: " + request.categoriaPublicId()));

        List<Author> authors = new ArrayList<>();
        for (UUID authorPublicId : request.autoresPublicIds()) {
            Author author = authorRepository.findByPublicId(authorPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Autor no encontrado con publicId: " + authorPublicId));
            authors.add(author);
        }

        Book book = bookMapper.toEntity(request);
        book.setPublicId(UUID.randomUUID());
        book.setIsbn(trimmedIsbn);
        book.setTitulo(request.titulo().trim());
        book.setStockTotal(request.stockTotal());
        book.setStockDisponible(request.stockTotal());
        book.setCategoria(category);
        book.setAutores(authors);

        Book saved = bookRepository.save(book);
        return bookMapper.toResponse(saved);
    }

    public BookResponse getBookByPublicId(UUID publicId) {
        return bookRepository.findByPublicId(publicId)
                .map(bookMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con publicId: " + publicId));
    }

    @Transactional
    public BookResponse updateBook(UUID publicId, BookUpdateRequest request) {
        Book book = bookRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con publicId: " + publicId));

        String trimmedIsbn = request.isbn().trim();
        if (bookRepository.existsByIsbnAndPublicIdNot(trimmedIsbn, publicId)) {
            throw new BusinessRuleException("Ya existe un libro registrado con el ISBN ingresado");
        }

        Category category = categoryRepository.findByPublicId(request.categoriaPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con publicId: " + request.categoriaPublicId()));

        List<Author> authors = new ArrayList<>();
        for (UUID authorPublicId : request.autoresPublicIds()) {
            Author author = authorRepository.findByPublicId(authorPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Autor no encontrado con publicId: " + authorPublicId));
            authors.add(author);
        }

        int prestados = book.getStockTotal() - book.getStockDisponible();
        if (request.stockTotal() < prestados) {
            throw new BusinessRuleException("No se puede reducir el stock total por debajo del número de copias prestadas");
        }

        int nuevoDisponible = request.stockTotal() - prestados;

        book.setIsbn(trimmedIsbn);
        book.setTitulo(request.titulo().trim());
        book.setStockTotal(request.stockTotal());
        book.setStockDisponible(nuevoDisponible);
        book.setCategoria(category);
        book.setAutores(authors);

        Book updated = bookRepository.save(book);
        return bookMapper.toResponse(updated);
    }

    public Page<BookResponse> searchBooks(
            String titulo,
            UUID categoriaPublicId,
            UUID autorPublicId,
            Boolean soloDisponibles,
            Pageable pageable) {

        Pageable effectivePageable = (pageable == null || pageable.isUnpaged())
                ? PageRequest.of(0, 10, Sort.by("titulo").ascending())
                : pageable;

        Specification<Book> spec = BookSpecification.withFilters(titulo, categoriaPublicId, autorPublicId, soloDisponibles);
        return bookRepository.findAll(spec, effectivePageable).map(bookMapper::toResponse);
    }

    @Transactional
    public BookResponse adjustStock(UUID publicId, com.EduardoMango.Biblioteca.feature.book.dto.StockAdjustmentRequest request) {
        Book book = bookRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con publicId: " + publicId));

        int prestados = book.getStockTotal() - book.getStockDisponible();
        if (request.nuevoStockTotal() < prestados) {
            throw new BusinessRuleException("El nuevo stock total no puede ser inferior a las " + prestados + " copias actualmente prestadas");
        }

        int nuevoDisponible = request.nuevoStockTotal() - prestados;
        book.setStockTotal(request.nuevoStockTotal());
        book.setStockDisponible(nuevoDisponible);

        Book saved = bookRepository.save(book);
        return bookMapper.toResponse(saved);
    }
}
