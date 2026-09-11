package com.EduardoMango.Biblioteca.feature.loan;

import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanResponse;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanReturnServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanMapper loanMapper;

    @InjectMocks
    private LoanService loanService;

    @Test
    @DisplayName("Escenario 1: Devolución a tiempo (Estado DEVUELTO)")
    void returnLoan_WhenOnTime_StatusDevueltoAndStockIncremented() {
        UUID loanPublicId = UUID.randomUUID();
        Book book = Book.builder()
                .id(1L)
                .stockDisponible(2)
                .build();

        Loan loan = Loan.builder()
                .id(10L)
                .publicId(loanPublicId)
                .libro(book)
                .fechaPrestamo(LocalDate.now().minusDays(5))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(9))
                .estado(LoanStatus.PRESTADO)
                .build();

        given(loanRepository.findByPublicId(loanPublicId)).willReturn(Optional.of(loan));
        given(loanRepository.save(any(Loan.class))).willAnswer(invocation -> invocation.getArgument(0));

        LoanResponse expectedResponse = new LoanResponse(
                loanPublicId, "978-1234567890", "Book", UUID.randomUUID(),
                loan.getFechaPrestamo(), loan.getFechaDevolucionEsperada(), LocalDate.now(), LoanStatus.DEVUELTO
        );
        given(loanMapper.toLoanResponse(any(Loan.class))).willReturn(expectedResponse);

        LoanResponse actual = loanService.returnLoan(loanPublicId);

        assertThat(actual.estado()).isEqualTo(LoanStatus.DEVUELTO);
        assertThat(loan.getEstado()).isEqualTo(LoanStatus.DEVUELTO);
        assertThat(loan.getFechaDevolucionEfectiva()).isEqualTo(LocalDate.now());
        assertThat(book.getStockDisponible()).isEqualTo(3);
        verify(bookRepository).save(book);
        verify(loanRepository).save(loan);
    }

    @Test
    @DisplayName("Escenario 2: Devolución fuera de término (Estado CON_RETRASO)")
    void returnLoan_WhenOverdue_StatusConRetrasoAndStockIncremented() {
        UUID loanPublicId = UUID.randomUUID();
        Book book = Book.builder()
                .id(1L)
                .stockDisponible(1)
                .build();

        Loan loan = Loan.builder()
                .id(10L)
                .publicId(loanPublicId)
                .libro(book)
                .fechaPrestamo(LocalDate.now().minusDays(20))
                .fechaDevolucionEsperada(LocalDate.now().minusDays(3))
                .estado(LoanStatus.PRESTADO)
                .build();

        given(loanRepository.findByPublicId(loanPublicId)).willReturn(Optional.of(loan));
        given(loanRepository.save(any(Loan.class))).willAnswer(invocation -> invocation.getArgument(0));

        LoanResponse expectedResponse = new LoanResponse(
                loanPublicId, "978-1234567890", "Book", UUID.randomUUID(),
                loan.getFechaPrestamo(), loan.getFechaDevolucionEsperada(), LocalDate.now(), LoanStatus.CON_RETRASO
        );
        given(loanMapper.toLoanResponse(any(Loan.class))).willReturn(expectedResponse);

        LoanResponse actual = loanService.returnLoan(loanPublicId);

        assertThat(actual.estado()).isEqualTo(LoanStatus.CON_RETRASO);
        assertThat(loan.getEstado()).isEqualTo(LoanStatus.CON_RETRASO);
        assertThat(loan.getFechaDevolucionEfectiva()).isEqualTo(LocalDate.now());
        assertThat(book.getStockDisponible()).isEqualTo(2);
        verify(bookRepository).save(book);
        verify(loanRepository).save(loan);
    }

    @Test
    @DisplayName("Escenario 3: Intento de devolver un préstamo ya procesado")
    void returnLoan_WhenAlreadyReturned_ThrowsBusinessRuleException() {
        UUID loanPublicId = UUID.randomUUID();
        Loan loan = Loan.builder()
                .publicId(loanPublicId)
                .estado(LoanStatus.DEVUELTO)
                .build();

        given(loanRepository.findByPublicId(loanPublicId)).willReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.returnLoan(loanPublicId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("El préstamo indicado ya ha sido devuelto anteriormente");
    }

    @Test
    @DisplayName("Devolución de préstamo inexistente lanza ResourceNotFoundException")
    void returnLoan_WhenNotFound_ThrowsResourceNotFoundException() {
        UUID loanPublicId = UUID.randomUUID();
        given(loanRepository.findByPublicId(loanPublicId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnLoan(loanPublicId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Préstamo no encontrado con publicId: " + loanPublicId);
    }
}

