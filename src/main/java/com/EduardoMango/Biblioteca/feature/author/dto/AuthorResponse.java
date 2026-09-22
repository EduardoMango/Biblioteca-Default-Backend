package com.EduardoMango.Biblioteca.feature.author.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Datos de respuesta que representan a un autor en el catálogo")
public record AuthorResponse(
        @Schema(description = "Identificador público inmutable del autor", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID publicId,

        @Schema(description = "Nombre(s) del autor", example = "Gabriel")
        String nombre,

        @Schema(description = "Apellido(s) del autor", example = "García Márquez")
        String apellido,

        @Schema(description = "Nacionalidad del autor", example = "Colombiana")
        String nacionalidad,

        @Schema(description = "Fecha de nacimiento", example = "1927-03-06")
        LocalDate fechaNacimiento
) {
}
