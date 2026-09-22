package com.EduardoMango.Biblioteca.feature.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para la creación o actualización de una categoría taxonómica de libros")
public record CategoryRequest(
        @Schema(description = "Nombre unívoco de la categoría (insensible a mayúsculas/minúsculas)",
                example = "Novela de Ficción",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
        String nombre,

        @Schema(description = "Descripción breve del género o alcance temático",
                example = "Obras narrativas de ficción literaria y novelas clásicas",
                maxLength = 255,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 255, message = "La descripción no puede exceder los 255 caracteres")
        String descripcion
) {
}
