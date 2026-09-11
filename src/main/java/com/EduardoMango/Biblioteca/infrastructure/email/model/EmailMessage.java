package com.EduardoMango.Biblioteca.infrastructure.email.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailMessage(
        @NotBlank @Email String to,
        @NotBlank String subject,
        @NotBlank String body
) {
}

