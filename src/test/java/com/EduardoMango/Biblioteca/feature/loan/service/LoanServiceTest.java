package com.EduardoMango.Biblioteca.feature.loan.service;

import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.book.domain.Book;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanCreateRequest;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanResponse;
import com.EduardoMango.Biblioteca.feature.loan.mapper.LoanMapper;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.model.entity.CredentialsEntity;
import com.EduardoMango.Biblioteca.model.entity.UserEntity;
import com.EduardoMango.Biblioteca.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CredentialsRepository credentialsRepository;

    @Mock
    private LoanMapper loanMapper;

    @InjectMocks
    private LoanService loanService;

    @Test
    @DisplayName("Escenario 1: Registro exitoso de préstamo por un Socio")
    void createLoan_WhenValidSocioRequest_Success() {
        UUID socioPublicId = UUID.randomUUID();
        UserEntity socio = UserEntity.builder()
                .id(1L)
                .publicId(socioPublicId)
                .nombre("Juan")
                .rol("SOCIO")
                .activo(true)
                .build();
        CredentialsEntity credentials = CredentialsEntity.builder().usuario(socio).build();

        UUID bookPublicId = UUID.randomUUID();
        Book book = Book.builder()
                .id(10L)
                .publicId(bookPublicId)
                .titulo("Design Patterns")
                .stockTotal(3)
                .stockDisponible(3)
                .build();

        LoanCreateRequest request = new LoanCreateRequest(bookPublicId, null);

        given(credentialsRepository.findByUsername("socio")).willReturn(Optional.of(credentials));
        given(bookRepository.findByPublicId(bookPublicId)).willReturn(Optional.of(book));
        given(loanRepository.countByUsuarioAndEstadoIn(socio, List.of(LoanStatus.PRESTADO, LoanStatus.CON_RETRASO))).willReturn(1L);
        given(loanRepository.hasOverdueLoans(socio, LocalDate.now())).willReturn(false);
        given(loanRepository.save(any(Loan.class))).willAnswer(invocation -> invocation.getArgument(0));

        LoanResponse expectedResponse = new LoanResponse(
                UUID.randomUUID(), bookPublicId, "Design Patterns", socioPublicId,
                LocalDate.now(), LocalDate.now().plusDays(14), null, LoanStatus.PRESTADO
        );
        given(loanMapper.toLoanResponse(any(Loan.class))).willReturn(expectedResponse);

        LoanResponse actual = loanService.createLoan(request, "socio");

        assertThat(actual).isNotNull();
        assertThat(actual.estado()).isEqualTo(LoanStatus.PRESTADO);
        assertThat(book.getStockDisponible()).isEqualTo(2);
        verify(bookRepository).save(book);
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    @DisplayName("Escenario 2: Bibliotecario registra préstamo en nombre de un socio")
    void createLoan_WhenAdminRegistersForSocio_Success() {
        UserEntity admin = UserEntity.builder()
                .publicId(UUID.randomUUID())
                .rol("BIBLIOTECARIO")
                .activo(true)
                .build();
        CredentialsEntity adminCredentials = CredentialsEntity.builder().usuario(admin).build();

        UUID targetSocioPublicId = UUID.randomUUID();
        UserEntity targetSocio = UserEntity.builder()
                .publicId(targetSocioPublicId)
                .nombre("Destino")
                .rol("SOCIO")
                .activo(true)
                .build();

        UUID bookPublicId = UUID.randomUUID();
        Book book = Book.builder()
                .publicId(bookPublicId)
                .stockDisponible(2)
                .build();

        LoanCreateRequest request = new LoanCreateRequest(bookPublicId, targetSocioPublicId);

        given(credentialsRepository.findByUsername("admin")).willReturn(Optional.of(adminCredentials));
        given(userRepository.findByPublicId(targetSocioPublicId)).willReturn(Optional.of(targetSocio));
        given(bookRepository.findByPublicId(bookPublicId)).willReturn(Optional.of(book));
        given(loanRepository.countByUsuarioAndEstadoIn(targetSocio, List.of(LoanStatus.PRESTADO, LoanStatus.CON_RETRASO))).willReturn(0L);
        given(loanRepository.hasOverdueLoans(targetSocio, LocalDate.now())).willReturn(false);
        given(loanRepository.save(any(Loan.class))).willAnswer(invocation -> invocation.getArgument(0));

        loanService.createLoan(request, "admin");

        assertThat(book.getStockDisponible()).isEqualTo(1);
        verify(bookRepository).save(book);
    }

    @Test
    @DisplayName("Escenario 3: Rechazo por falta de stock disponible")
    void createLoan_WhenNoStock_ThrowsBusinessRuleException() {
        UserEntity socio = UserEntity.builder().publicId(UUID.randomUUID()).activo(true).rol("SOCIO").build();
        CredentialsEntity credentials = CredentialsEntity.builder().usuario(socio).build();

        UUID bookPublicId = UUID.randomUUID();
        Book book = Book.builder().publicId(bookPublicId).stockDisponible(0).build();

        LoanCreateRequest request = new LoanCreateRequest(bookPublicId, null);

        given(credentialsRepository.findByUsername("socio")).willReturn(Optional.of(credentials));
        given(bookRepository.findByPublicId(bookPublicId)).willReturn(Optional.of(book));

        assertThatThrownBy(() -> loanService.createLoan(request, "socio"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No hay ejemplares disponibles del libro solicitado");
    }

    @Test
    @DisplayName("Escenario 4: Rechazo por superar el límite máximo de préstamos activos (3)")
    void createLoan_WhenMaxActiveLoansReached_ThrowsBusinessRuleException() {
        UserEntity socio = UserEntity.builder().publicId(UUID.randomUUID()).activo(true).rol("SOCIO").build();
        CredentialsEntity credentials = CredentialsEntity.builder().usuario(socio).build();

        UUID bookPublicId = UUID.randomUUID();
        Book book = Book.builder().publicId(bookPublicId).stockDisponible(3).build();

        LoanCreateRequest request = new LoanCreateRequest(bookPublicId, null);

        given(credentialsRepository.findByUsername("socio")).willReturn(Optional.of(credentials));
        given(bookRepository.findByPublicId(bookPublicId)).willReturn(Optional.of(book));
        given(loanRepository.countByUsuarioAndEstadoIn(socio, List.of(LoanStatus.PRESTADO, LoanStatus.CON_RETRASO))).willReturn(3L);

        assertThatThrownBy(() -> loanService.createLoan(request, "socio"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("El usuario ha alcanzado el límite máximo de 3 préstamos activos");
    }

    @Test
    @DisplayName("Escenario 5: Rechazo por poseer préstamos vencidos sin devolver")
    void createLoan_WhenHasOverdueLoans_ThrowsBusinessRuleException() {
        UserEntity socio = UserEntity.builder().publicId(UUID.randomUUID()).activo(true).rol("SOCIO").build();
        CredentialsEntity credentials = CredentialsEntity.builder().usuario(socio).build();

        UUID bookPublicId = UUID.randomUUID();
        Book book = Book.builder().publicId(bookPublicId).stockDisponible(3).build();

        LoanCreateRequest request = new LoanCreateRequest(bookPublicId, null);

        given(credentialsRepository.findByUsername("socio")).willReturn(Optional.of(credentials));
        given(bookRepository.findByPublicId(bookPublicId)).willReturn(Optional.of(book));
        given(loanRepository.countByUsuarioAndEstadoIn(socio, List.of(LoanStatus.PRESTADO, LoanStatus.CON_RETRASO))).willReturn(1L);
        given(loanRepository.hasOverdueLoans(socio, LocalDate.now())).willReturn(true);

        assertThatThrownBy(() -> loanService.createLoan(request, "socio"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("El usuario posee préstamos atrasados pendientes de devolución");
    }
}

