package com.EduardoMango.Biblioteca.feature.user.service;

import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.auth.domain.CredentialsEntity;
import com.EduardoMango.Biblioteca.feature.auth.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import com.EduardoMango.Biblioteca.feature.user.UserRepository;
import com.EduardoMango.Biblioteca.feature.user.dto.LoanHistoryResponse;
import com.EduardoMango.Biblioteca.feature.user.dto.UserProfileResponse;
import com.EduardoMango.Biblioteca.feature.user.mapper.UserProfileMapper;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CredentialsRepository credentialsRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    @DisplayName("Dado un usuario autenticado existente, Cuando consulta su perfil, Entonces retorna sus datos y préstamos mapeados")
    void getUserProfile_WhenUserExists_ReturnsProfileWithLoans() {
        String username = "juan_perez";
        UUID userPublicId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(1L)
                .publicId(userPublicId)
                .nombre("Juan")
                .apellido("Pérez")
                .email("juan@test.com")
                .rol("SOCIO")
                .activo(true)
                .build();

        CredentialsEntity credentials = CredentialsEntity.builder()
                .id(1L)
                .username(username)
                .usuario(user)
                .build();

        UUID loanPublicId = UUID.randomUUID();
        Book book = Book.builder().titulo("Clean Code").build();
        Loan loan = Loan.builder()
                .id(1L)
                .publicId(loanPublicId)
                .libro(book)
                .usuario(user)
                .fechaPrestamo(LocalDate.now().minusDays(5))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(9))
                .estado(LoanStatus.PRESTADO)
                .build();

        LoanHistoryResponse loanHistoryResponse = new LoanHistoryResponse(
                loanPublicId, "Clean Code", loan.getFechaPrestamo(), loan.getFechaDevolucionEsperada(), null, LoanStatus.PRESTADO
        );

        UserProfileResponse expectedProfile = new UserProfileResponse(
                userPublicId, "Juan", "Pérez", "juan@test.com", "SOCIO", List.of(loanHistoryResponse)
        );

        given(credentialsRepository.findByUsername(username)).willReturn(Optional.of(credentials));
        given(loanRepository.findByUsuarioOrderByFechaPrestamoDesc(user)).willReturn(List.of(loan));
        given(userProfileMapper.toLoanHistoryResponseList(List.of(loan))).willReturn(List.of(loanHistoryResponse));
        given(userProfileMapper.toUserProfileResponse(user, List.of(loanHistoryResponse))).willReturn(expectedProfile);

        UserProfileResponse actualProfile = userProfileService.getUserProfile(username);

        assertThat(actualProfile).isNotNull();
        assertThat(actualProfile.publicId()).isEqualTo(userPublicId);
        assertThat(actualProfile.nombre()).isEqualTo("Juan");
        assertThat(actualProfile.prestamos()).hasSize(1);
        assertThat(actualProfile.prestamos().getFirst().tituloLibro()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("Dado un usuario inexistente, Cuando consulta su perfil, Entonces lanza ResourceNotFoundException")
    void getUserProfile_WhenUserDoesNotExist_ThrowsResourceNotFoundException() {
        String username = "no_existe";
        given(credentialsRepository.findByUsername(username)).willReturn(Optional.empty());
        given(userRepository.findByEmail(username)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getUserProfile(username))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado: " + username);
    }

    @Test
    @DisplayName("Dado un usuario autenticado, Cuando solicita sus préstamos paginados, Entonces retorna la página correspondiente")
    void getUserLoans_WhenCalled_ReturnsPagedLoans() {
        String username = "juan_perez";
        UserEntity user = UserEntity.builder().id(1L).email("juan@test.com").build();
        CredentialsEntity credentials = CredentialsEntity.builder().usuario(user).build();

        Pageable pageable = PageRequest.of(0, 5);
        Loan loan = Loan.builder().id(1L).build();
        Page<Loan> loanPage = new PageImpl<>(List.of(loan), pageable, 1);

        LoanHistoryResponse loanResponse = new LoanHistoryResponse(
                UUID.randomUUID(), "Refactoring", LocalDate.now(), LocalDate.now().plusDays(14), null, LoanStatus.PRESTADO
        );

        given(credentialsRepository.findByUsername(username)).willReturn(Optional.of(credentials));
        given(loanRepository.findByUsuarioOrderByFechaPrestamoDesc(user, pageable)).willReturn(loanPage);
        given(userProfileMapper.toLoanHistoryResponse(loan)).willReturn(loanResponse);

        Page<LoanHistoryResponse> result = userProfileService.getUserLoans(username, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().tituloLibro()).isEqualTo("Refactoring");
    }
}

