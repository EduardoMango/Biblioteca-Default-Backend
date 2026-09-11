package com.EduardoMango.Biblioteca.feature.loan;

import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanCreateRequest;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanResponse;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.feature.auth.domain.CredentialsEntity;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import com.EduardoMango.Biblioteca.feature.auth.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.feature.user.UserRepository;
import lombok.RequiredArgsConstructor;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanSupervisionResponse;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.EduardoMango.Biblioteca.feature.loan.event.LoanCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final LoanMapper loanMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoanResponse createLoan(LoanCreateRequest request, String authenticatedPrincipalName) {
        UserEntity authenticatedUser = findUserByPrincipal(authenticatedPrincipalName);

        UserEntity targetUser;
        if (request.usuarioPublicId() != null) {
            if ("BIBLIOTECARIO".equalsIgnoreCase(authenticatedUser.getRol())) {
                targetUser = userRepository.findByPublicId(request.usuarioPublicId())
                        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con publicId: " + request.usuarioPublicId()));
            } else {
                if (!authenticatedUser.getPublicId().equals(request.usuarioPublicId())) {
                    throw new BusinessRuleException("Un socio no puede registrar préstamos en nombre de otro usuario");
                }
                targetUser = authenticatedUser;
            }
        } else {
            targetUser = authenticatedUser;
        }

        if (Boolean.FALSE.equals(targetUser.getActivo())) {
            throw new BusinessRuleException("El usuario se encuentra inactivo y no puede solicitar préstamos");
        }

        Book book = bookRepository.findByIsbn(request.libroIsbn())
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con ISBN: " + request.libroIsbn()));

        if (book.getStockDisponible() == null || book.getStockDisponible() <= 0) {
            throw new BusinessRuleException("No hay ejemplares disponibles del libro solicitado");
        }

        long activeLoans = loanRepository.countByUsuarioAndEstadoIn(
                targetUser, List.of(LoanStatus.PRESTADO, LoanStatus.CON_RETRASO));
        if (activeLoans >= 3) {
            throw new BusinessRuleException("El usuario ha alcanzado el límite máximo de 3 préstamos activos");
        }

        boolean hasOverdue = loanRepository.hasOverdueLoans(targetUser, LocalDate.now());
        if (hasOverdue) {
            throw new BusinessRuleException("El usuario posee préstamos atrasados pendientes de devolución");
        }

        LocalDate fechaPrestamo = LocalDate.now();
        LocalDate fechaDevolucionEsperada = fechaPrestamo.plusDays(14);

        Loan loan = Loan.builder()
                .libro(book)
                .usuario(targetUser)
                .fechaPrestamo(fechaPrestamo)
                .fechaDevolucionEsperada(fechaDevolucionEsperada)
                .estado(LoanStatus.PRESTADO)
                .build();

        book.setStockDisponible(book.getStockDisponible() - 1);
        bookRepository.save(book);

        Loan savedLoan = loanRepository.save(loan);
        eventPublisher.publishEvent(new LoanCreatedEvent(
                savedLoan.getPublicId(),
                targetUser.getEmail(),
                targetUser.getNombre() + " " + targetUser.getApellido(),
                book.getTitulo(),
                savedLoan.getFechaPrestamo(),
                savedLoan.getFechaDevolucionEsperada()
        ));
        return loanMapper.toLoanResponse(savedLoan);
    }

    @Transactional
    public LoanResponse returnLoan(UUID loanPublicId) {
        Loan loan = loanRepository.findByPublicId(loanPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con publicId: " + loanPublicId));

        if (loan.getEstado() != LoanStatus.PRESTADO) {
            throw new BusinessRuleException("El préstamo indicado ya ha sido devuelto anteriormente");
        }

        LocalDate today = LocalDate.now();
        loan.setFechaDevolucionEfectiva(today);

        if (today.isAfter(loan.getFechaDevolucionEsperada())) {
            loan.setEstado(LoanStatus.CON_RETRASO);
        } else {
            loan.setEstado(LoanStatus.DEVUELTO);
        }

        Book book = loan.getLibro();
        book.setStockDisponible(book.getStockDisponible() + 1);
        bookRepository.save(book);

        Loan savedLoan = loanRepository.save(loan);
        return loanMapper.toLoanResponse(savedLoan);
    }

    public Page<LoanSupervisionResponse> getLoansForSupervision(
            LoanStatus estado,
            Boolean soloAtrasados,
            UUID usuarioPublicId,
            Pageable pageable) {
        Page<Loan> loansPage = loanRepository.findAll(
                LoanSpecification.withFilters(estado, soloAtrasados, usuarioPublicId), pageable);
        return loansPage.map(loanMapper::toLoanSupervisionResponse);
    }

    private UserEntity findUserByPrincipal(String principalName) {
        return credentialsRepository.findByUsername(principalName)
                .map(CredentialsEntity::getUsuario)
                .or(() -> userRepository.findByEmail(principalName))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + principalName));
    }
}

