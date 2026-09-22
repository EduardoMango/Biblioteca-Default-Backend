package com.EduardoMango.Biblioteca.feature.author.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Datos para la creación o actualización de un autor en el catálogo")
public record AuthorRequest(
        @Schema(description = "Nombre(s) del autor", example = "Gabriel", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre no puede estar vacío")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @Schema(description = "Apellido(s) del autor", example = "García Márquez", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El apellido no puede estar en blanco")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String apellido,

        @Schema(description = "País de origen o nacionalidad del autor", example = "Colombiana", maxLength = 100, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "La nacionalidad no puede exceder 100 caracteres")
        String nacionalidad,

        @Schema(description = "Fecha de nacimiento del autor", example = "1927-03-06", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate fechaNacimiento
) {
}
