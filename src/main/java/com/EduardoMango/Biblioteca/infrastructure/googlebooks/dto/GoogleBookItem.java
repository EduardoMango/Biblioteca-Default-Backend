package com.EduardoMango.Biblioteca.infrastructure.googlebooks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleBookItem(
        String id,
        GoogleBookVolumeInfo volumeInfo
) {}

