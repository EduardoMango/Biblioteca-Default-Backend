package com.EduardoMango.Biblioteca.feature.book.domain;

import com.EduardoMango.Biblioteca.feature.author.domain.Author;
import com.EduardoMango.Biblioteca.feature.category.domain.Category;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private Integer stockTotal;

    @Column(nullable = false)
    private Integer stockDisponible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category categoria;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @Builder.Default
    private List<Author> autores = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
        if (this.stockDisponible == null && this.stockTotal != null) {
            this.stockDisponible = this.stockTotal;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return publicId != null && Objects.equals(publicId, book.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }
}

