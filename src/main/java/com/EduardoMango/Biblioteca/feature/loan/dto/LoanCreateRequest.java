package com.EduardoMango.Biblioteca.feature.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@Schema(description = "Solicitud para registrar un nuevo préstamo de libro")
public record LoanCreateRequest(
        @Schema(description = "Código ISBN del libro solicitado para préstamo", example = "9780307474728", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El ISBN del libro es obligatorio")
        String libroIsbn,

        @Schema(description = "Identificador público del usuario socio (opcional para bibliotecarios prestando a un tercero; si se omite, se asigna al usuario autenticado)",
                example = "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID usuarioPublicId
) {
}
