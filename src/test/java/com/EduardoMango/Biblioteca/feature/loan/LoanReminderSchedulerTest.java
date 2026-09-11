package com.EduardoMango.Biblioteca.feature.loan;

import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.feature.loan.scheduler.LoanReminderScheduler;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import com.EduardoMango.Biblioteca.infrastructure.email.model.EmailMessage;
import com.EduardoMango.Biblioteca.infrastructure.email.port.out.EmailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanReminderSchedulerTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private LoanReminderScheduler scheduler;

    @Test
    @DisplayName("Escenario 1: Notificación a socios de préstamos que vencen en 48 horas")
    void sendDueSoonLoanReminders_WhenLoansDueIn48Hours_SendsEmails() {
        LocalDate dueDate = LocalDate.now().plusDays(2);

        UserEntity user1 = UserEntity.builder()
                .nombre("Martin")
                .apellido("Fowler")
                .email("martin@refactoring.com")
                .build();
        Book book1 = Book.builder().titulo("Refactoring").build();
        Loan loan1 = Loan.builder()
                .publicId(UUID.randomUUID())
                .usuario(user1)
                .libro(book1)
                .fechaDevolucionEsperada(dueDate)
                .estado(LoanStatus.PRESTADO)
                .build();

        UserEntity user2 = UserEntity.builder()
                .nombre("Kent")
                .apellido("Beck")
                .email("kent@tdd.com")
                .build();
        Book book2 = Book.builder().titulo("TDD by Example").build();
        Loan loan2 = Loan.builder()
                .publicId(UUID.randomUUID())
                .usuario(user2)
                .libro(book2)
                .fechaDevolucionEsperada(dueDate)
                .estado(LoanStatus.PRESTADO)
                .build();

        given(loanRepository.findActiveLoansDueAt(eq(LoanStatus.PRESTADO), eq(dueDate)))
                .willReturn(List.of(loan1, loan2));

        scheduler.sendDueSoonLoanReminders();

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, times(2)).send(captor.capture());

        List<EmailMessage> messages = captor.getAllValues();
        assertThat(messages).hasSize(2);

        assertThat(messages.get(0).to()).isEqualTo("martin@refactoring.com");
        assertThat(messages.get(0).subject()).contains("Refactoring");
        assertThat(messages.get(0).body()).contains("Martin Fowler");
        assertThat(messages.get(0).body()).contains("Refactoring");

        assertThat(messages.get(1).to()).isEqualTo("kent@tdd.com");
        assertThat(messages.get(1).subject()).contains("TDD by Example");
        assertThat(messages.get(1).body()).contains("Kent Beck");
        assertThat(messages.get(1).body()).contains("TDD by Example");
    }

    @Test
    @DisplayName("Escenario 2: Omisión cuando no hay préstamos que vencen en 48 horas")
    void sendDueSoonLoanReminders_WhenNoLoansDue_NoEmailsSent() {
        LocalDate dueDate = LocalDate.now().plusDays(2);
        given(loanRepository.findActiveLoansDueAt(eq(LoanStatus.PRESTADO), eq(dueDate)))
                .willReturn(Collections.emptyList());

        scheduler.sendDueSoonLoanReminders();

        verify(emailSender, never()).send(any());
    }

    @Test
    @DisplayName("RN 4: Tolerancia a errores - si falla un envío particular, continúa con el siguiente")
    void sendDueSoonLoanReminders_WhenOneFails_ContinuesWithNext() {
        LocalDate dueDate = LocalDate.now().plusDays(2);

        UserEntity user1 = UserEntity.builder()
                .nombre("User")
                .apellido("One")
                .email("error@test.com")
                .build();
        Book book1 = Book.builder().titulo("Libro 1").build();
        Loan loan1 = Loan.builder()
                .publicId(UUID.randomUUID())
                .usuario(user1)
                .libro(book1)
                .fechaDevolucionEsperada(dueDate)
                .estado(LoanStatus.PRESTADO)
                .build();

        UserEntity user2 = UserEntity.builder()
                .nombre("User")
                .apellido("Two")
                .email("success@test.com")
                .build();
        Book book2 = Book.builder().titulo("Libro 2").build();
        Loan loan2 = Loan.builder()
                .publicId(UUID.randomUUID())
                .usuario(user2)
                .libro(book2)
                .fechaDevolucionEsperada(dueDate)
                .estado(LoanStatus.PRESTADO)
                .build();

        given(loanRepository.findActiveLoansDueAt(eq(LoanStatus.PRESTADO), eq(dueDate)))
                .willReturn(List.of(loan1, loan2));

        doThrow(new RuntimeException("Simulated mail failure"))
                .doNothing()
                .when(emailSender).send(any(EmailMessage.class));

        scheduler.sendDueSoonLoanReminders();

        verify(emailSender, times(2)).send(any(EmailMessage.class));
    }
}

