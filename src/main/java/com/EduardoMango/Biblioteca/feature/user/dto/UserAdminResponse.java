package com.EduardoMango.Biblioteca.feature.user.dto;

import java.util.UUID;

public record UserAdminResponse(
        UUID publicId,
        String nombre,
        String apellido,
        String email,
        String rol,
        Boolean activo
) {
}

