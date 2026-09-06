package com.EduardoMango.Biblioteca.feature.loan.service;

import com.EduardoMango.Biblioteca.feature.book.domain.Book;
import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.dto.BookLoanSummary;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanSupervisionResponse;
import com.EduardoMango.Biblioteca.feature.loan.dto.UserLoanSummary;
import com.EduardoMango.Biblioteca.feature.loan.mapper.LoanMapper;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.model.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LoanSupervisionServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanMapper loanMapper;

    @InjectMocks
    private LoanService loanService;

    @Test
    @DisplayName("Supervisión de préstamos: retorna página mapeada correctamente")
    void getLoansForSupervision_WhenCalled_ReturnsPagedSupervisionResponses() {
        UUID loanPublicId = UUID.randomUUID();
        UUID userPublicId = UUID.randomUUID();
        UUID bookPublicId = UUID.randomUUID();

        UserEntity user = UserEntity.builder().publicId(userPublicId).nombre("Socio").email("s@test.com").build();
        Book book = Book.builder().publicId(bookPublicId).titulo("Effective Java").isbn("1234567890").build();

        Loan loan = Loan.builder()
                .publicId(loanPublicId)
                .usuario(user)
                .libro(book)
                .fechaPrestamo(LocalDate.now().minusDays(20))
                .fechaDevolucionEsperada(LocalDate.now().minusDays(6))
                .estado(LoanStatus.PRESTADO)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Loan> loanPage = new PageImpl<>(List.of(loan), pageable, 1);

        LoanSupervisionResponse expectedResponse = new LoanSupervisionResponse(
                loanPublicId,
                new UserLoanSummary(userPublicId, "Socio", "s@test.com"),
                new BookLoanSummary(bookPublicId, "Effective Java", "1234567890"),
                loan.getFechaPrestamo(),
                loan.getFechaDevolucionEsperada(),
                null,
                LoanStatus.PRESTADO,
                6L
        );

        given(loanRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(loanPage);
        given(loanMapper.toLoanSupervisionResponse(loan)).willReturn(expectedResponse);

        Page<LoanSupervisionResponse> result = loanService.getLoansForSupervision(LoanStatus.PRESTADO, true, userPublicId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().publicId()).isEqualTo(loanPublicId);
        assertThat(result.getContent().getFirst().diasAtraso()).isEqualTo(6L);
    }
}

