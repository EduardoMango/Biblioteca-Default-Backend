package com.EduardoMango.Biblioteca.infrastructure.email.port.out;

import com.EduardoMango.Biblioteca.infrastructure.email.model.EmailMessage;

public interface EmailSender {
    void send(EmailMessage emailMessage);
}

