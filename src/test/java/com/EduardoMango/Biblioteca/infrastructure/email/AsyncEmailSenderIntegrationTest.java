package com.EduardoMango.Biblioteca.infrastructure.email;

import com.EduardoMango.Biblioteca.infrastructure.email.model.EmailMessage;
import com.EduardoMango.Biblioteca.infrastructure.email.port.out.EmailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AsyncEmailSenderIntegrationTest {

    @Autowired
    private EmailSender emailSender;

    @Test
    @DisplayName("Criterio: El bean EmailSender está configurado y responde")
    void testEmailSenderBeanExists() {
        assertThat(emailSender).isNotNull();
        // Llamada failsafe que no debe lanzar excepción
        emailSender.send(new EmailMessage("test@async.com", "Test Async", "<p>Body</p>"));
    }
}

