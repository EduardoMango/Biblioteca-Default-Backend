package com.EduardoMango.Biblioteca.feature.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Solicitud para activar o desactivar una cuenta de usuario")
public record UserStatusUpdateRequest(
        @Schema(description = "Nuevo estado de la cuenta (true = activa / habilitada, false = suspendida / desactivada)",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}
