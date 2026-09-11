package com.EduardoMango.Biblioteca.feature.loan;

import com.EduardoMango.Biblioteca.feature.loan.event.LoanCreatedEvent;
import com.EduardoMango.Biblioteca.feature.loan.listener.LoanNotificationListener;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanNotificationListenerTest {

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private LoanNotificationListener listener;

    @Test
    @DisplayName("Escenario 1: Envío automático de correo con datos del préstamo al recibir LoanCreatedEvent")
    void handleLoanCreated_Success() {
        UUID loanId = UUID.randomUUID();
        LoanCreatedEvent event = new LoanCreatedEvent(
                loanId,
                "socio@ejemplo.com",
                "Juan Perez",
                "Clean Architecture",
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 22)
        );

        listener.handleLoanCreated(event);

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, times(1)).send(captor.capture());

        EmailMessage message = captor.getValue();
        assertThat(message.to()).isEqualTo("socio@ejemplo.com");
        assertThat(message.subject()).contains("Clean Architecture");
        assertThat(message.body()).contains("Juan Perez");
        assertThat(message.body()).contains("Clean Architecture");
        assertThat(message.body()).contains(loanId.toString());
        assertThat(message.body()).contains("2026-09-08");
        assertThat(message.body()).contains("2026-09-22");
    }
}

