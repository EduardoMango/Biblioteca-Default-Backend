package com.EduardoMango.Biblioteca.feature.loan.domain;

import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book libro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity usuario;

    @Column(nullable = false)
    private LocalDate fechaPrestamo;

    @Column(nullable = false)
    private LocalDate fechaDevolucionEsperada;

    @Column
    private LocalDate fechaDevolucionEfectiva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus estado;

    @PrePersist
    public void prePersist() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
        if (this.fechaPrestamo == null) {
            this.fechaPrestamo = LocalDate.now();
        }
        if (this.fechaDevolucionEsperada == null) {
            this.fechaDevolucionEsperada = this.fechaPrestamo.plusDays(14);
        }
        if (this.estado == null) {
            this.estado = LoanStatus.PRESTADO;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Loan loan = (Loan) o;
        return publicId != null && Objects.equals(publicId, loan.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }
}

