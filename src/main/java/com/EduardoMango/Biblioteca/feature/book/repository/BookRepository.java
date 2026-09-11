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

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    boolean existsByCategoria_PublicId(UUID categoriaPublicId);

    boolean existsByAutores_PublicId(UUID autorPublicId);

    void deleteByIsbn(String isbn);
}

