package com.EduardoMango.Biblioteca.feature.book.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
        @NotNull(message = "El nuevo stock total es obligatorio")
        @Min(value = 0, message = "El nuevo stock total no puede ser negativo")
        Integer nuevoStockTotal
) {
}

