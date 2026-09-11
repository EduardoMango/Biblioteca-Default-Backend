package com.EduardoMango.Biblioteca.feature.loan.repository;

import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

    List<Loan> findByUsuarioOrderByFechaPrestamoDesc(UserEntity usuario);

    Page<Loan> findByUsuarioOrderByFechaPrestamoDesc(UserEntity usuario, Pageable pageable);

    Optional<Loan> findByPublicId(UUID publicId);

    long countByUsuarioAndEstadoIn(UserEntity usuario, Collection<LoanStatus> estados);

    boolean existsByUsuarioAndEstadoIn(UserEntity usuario, Collection<LoanStatus> estados);

    @Query("SELECT (COUNT(l) > 0) FROM Loan l WHERE l.usuario = :usuario AND l.fechaDevolucionEfectiva IS NULL AND l.fechaDevolucionEsperada < :fecha")
    boolean hasOverdueLoans(@Param("usuario") UserEntity usuario, @Param("fecha") LocalDate fecha);

    @Query("SELECT l FROM Loan l JOIN FETCH l.usuario JOIN FETCH l.libro WHERE l.estado = :estado AND l.fechaDevolucionEsperada = :fecha")
    List<Loan> findActiveLoansDueAt(@Param("estado") LoanStatus estado, @Param("fecha") LocalDate fecha);
}

