package com.EduardoMango.Biblioteca.feature.user.event;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userPublicId,
        String email,
        String nombre
) {
}

