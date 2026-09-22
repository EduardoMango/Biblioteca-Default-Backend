package com.EduardoMango.Biblioteca.infrastructure.googlebooks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleBooksResponse(
        String kind,
        Integer totalItems,
        List<GoogleBookItem> items
) {}

