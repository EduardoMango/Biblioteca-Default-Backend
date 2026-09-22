package com.EduardoMango.Biblioteca.feature.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Solicitud para importar automáticamente un libro desde Google Books API hacia el catálogo")
public record ImportBookRequest(
        @Schema(description = "Código ISBN del libro a consultar e importar", example = "9780307474728", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El ISBN es obligatorio")
        String isbn,

        @Schema(description = "Cantidad de copias físicas a registrar en el stock inicial", example = "3", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El número de copias es obligatorio")
        @Min(value = 1, message = "El total de copias debe ser al menos 1")
        Integer totalCopies
) {}
