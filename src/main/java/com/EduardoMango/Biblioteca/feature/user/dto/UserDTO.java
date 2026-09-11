package com.EduardoMango.Biblioteca.feature.user.dto;

import java.util.Set;
import java.util.UUID;

public record UserDTO(
        UUID publicId,
        String username,
        String nombre,
        String apellido,
        String email,
        String dni,
        String telefono,
        Set<String> roles
) {
}

