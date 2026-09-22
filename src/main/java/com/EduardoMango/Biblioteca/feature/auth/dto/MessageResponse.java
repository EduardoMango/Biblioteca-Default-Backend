package com.EduardoMango.Biblioteca.feature.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta genérica informativa o de confirmación de operación")
public record MessageResponse(
        @Schema(description = "Mensaje descriptivo del resultado de la operación",
                example = "Cuenta activada exitosamente")
        String message
) {
}
