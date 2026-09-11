package com.EduardoMango.Biblioteca.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "El nombre de usuario no puede estar vacío")
        String username,

        @NotBlank(message = "La contraseña no puede estar vacía")
        String password
) {
}

