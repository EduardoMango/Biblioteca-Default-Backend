package com.EduardoMango.Biblioteca.feature.book.repository;

import com.EduardoMango.Biblioteca.feature.author.Author;
import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.category.Category;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BookSpecification {

    public static Specification<Book> withFilters(
            String titulo,
            UUID categoriaPublicId,
            UUID autorPublicId,
            Boolean soloDisponibles) {
        return withFilters(titulo, null, categoriaPublicId, autorPublicId, soloDisponibles);
    }

    public static Specification<Book> withFilters(
            String titulo,
            String isbn,
            UUID categoriaPublicId,
            UUID autorPublicId,
            Boolean soloDisponibles) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (titulo != null && !titulo.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("titulo")),
                        "%" + titulo.trim().toLowerCase() + "%"
                ));
            }

            if (isbn != null && !isbn.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("isbn")),
                        "%" + isbn.trim().toLowerCase() + "%"
                ));
            }

            if (categoriaPublicId != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoria").get("publicId"), categoriaPublicId));
            }

            if (autorPublicId != null) {
                Join<Book, Author> authorJoin = root.join("autores");
                predicates.add(criteriaBuilder.equal(authorJoin.get("publicId"), autorPublicId));
            }

            if (Boolean.TRUE.equals(soloDisponibles)) {
                predicates.add(criteriaBuilder.greaterThan(root.get("stockDisponible"), 0));
            }

            if (query != null) {
                query.distinct(true);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
