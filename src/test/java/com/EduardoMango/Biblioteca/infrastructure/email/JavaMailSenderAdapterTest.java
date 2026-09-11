package com.EduardoMango.Biblioteca.infrastructure.email;

import com.EduardoMango.Biblioteca.infrastructure.email.adapter.out.JavaMailSenderAdapter;
import com.EduardoMango.Biblioteca.infrastructure.email.model.EmailMessage;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JavaMailSenderAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private JavaMailSenderAdapter emailSender;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Escenario 1: Envío exitoso de un email HTML asíncrono")
    void testEnvioExitosoEmailHtml() throws Exception {
        // Given
        EmailMessage emailMessage = new EmailMessage(
                "usuario@correo.com",
                "Bienvenido a la Biblioteca",
                "<h1>Hola Mundo</h1>"
        );

        // When
        emailSender.send(emailMessage);

        // Then
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        sentMessage.saveChanges();
        assertThat(sentMessage.getAllRecipients()[0].toString()).isEqualTo("usuario@correo.com");
        assertThat(sentMessage.getSubject()).isEqualTo("Bienvenido a la Biblioteca");
        assertThat(sentMessage.getContent().toString()).contains("<h1>Hola Mundo</h1>");
        assertThat(sentMessage.getContentType()).contains("text/html");
    }

    @Test
    @DisplayName("Escenario 2: Tolerancia a fallos en el servidor SMTP (no propaga excepción)")
    void testToleranciaFallosServidorSmtp() {
        // Given
        EmailMessage emailMessage = new EmailMessage(
                "usuario@correo.com",
                "Recordatorio",
                "<p>Mensaje</p>"
        );
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThatCode(() -> emailSender.send(emailMessage))
                .doesNotThrowAnyException();

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}

