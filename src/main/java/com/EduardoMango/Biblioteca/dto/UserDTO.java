package com.EduardoMango.Biblioteca.dto;

import java.util.Set;

public record UserDTO(
        Long id,
        String username,
        String nombre,
        String apellido,
        String email,
        String dni,
        String telefono,
        Set<String> roles
) {
}

