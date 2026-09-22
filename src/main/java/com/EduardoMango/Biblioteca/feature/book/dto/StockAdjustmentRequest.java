package com.EduardoMango.Biblioteca.feature.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Solicitud para ajustar el stock físico total de un libro")
public record StockAdjustmentRequest(
        @Schema(description = "Nueva cantidad total de copias físicas (debe ser mayor o igual a las copias prestadas actualmente)",
                example = "12",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El nuevo stock total es obligatorio")
        @Min(value = 0, message = "El nuevo stock total no puede ser negativo")
        Integer nuevoStockTotal
) {
}
