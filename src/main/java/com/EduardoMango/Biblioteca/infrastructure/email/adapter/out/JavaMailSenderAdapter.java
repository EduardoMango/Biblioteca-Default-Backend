package com.EduardoMango.Biblioteca.infrastructure.email.adapter.out;

import com.EduardoMango.Biblioteca.infrastructure.email.model.EmailMessage;
import com.EduardoMango.Biblioteca.infrastructure.email.port.out.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JavaMailSenderAdapter implements EmailSender {

    private final JavaMailSender mailSender;

    @Async
    @Override
    public void send(EmailMessage emailMessage) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setTo(emailMessage.to());
            helper.setSubject(emailMessage.subject());
            helper.setText(emailMessage.body(), true);
            mailSender.send(mimeMessage);
            log.info("Email successfully sent to {}", emailMessage.to());
        } catch (MessagingException | MailException e) {
            log.error("Error sending email to recipient {}: {}", emailMessage.to(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to recipient {}: {}", emailMessage.to(), e.getMessage(), e);
        }
    }
}

