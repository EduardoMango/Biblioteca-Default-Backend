package com.EduardoMango.Biblioteca.infrastructure.googlebooks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleBookImageLinks(
        String smallThumbnail,
        String thumbnail
) {}

