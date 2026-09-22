package com.EduardoMango.Biblioteca.feature.book.repository;

import com.EduardoMango.Biblioteca.feature.book.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIsbn(String isbn);

    Optional<Book> findByPublicId(UUID publicId);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Book b LEFT JOIN FETCH b.categoria LEFT JOIN FETCH b.autores WHERE b.id = :id")
    Optional<Book> findWithDetailsById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Book b LEFT JOIN FETCH b.categoria LEFT JOIN FETCH b.autores WHERE b.publicId = :publicId")
    Optional<Book> findWithDetailsByPublicId(@org.springframework.data.repository.query.Param("publicId") UUID publicId);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    boolean existsByCategoria_PublicId(UUID categoriaPublicId);

    boolean existsByAutores_PublicId(UUID autorPublicId);

    void deleteByIsbn(String isbn);
}

