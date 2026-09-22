package com.EduardoMango.Biblioteca.feature.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Datos de respuesta que representan una categoría de libros")
public record CategoryResponse(
        @Schema(description = "Identificador público inmutable de la categoría",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID publicId,

        @Schema(description = "Nombre de la categoría",
                example = "Novela de Ficción")
        String nombre,

        @Schema(description = "Descripción de la categoría",
                example = "Obras narrativas de ficción literaria y novelas clásicas")
        String descripcion
) {
}
