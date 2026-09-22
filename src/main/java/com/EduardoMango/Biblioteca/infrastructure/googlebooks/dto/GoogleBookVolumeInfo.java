package com.EduardoMango.Biblioteca.infrastructure.googlebooks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleBookVolumeInfo(
        String title,
        List<String> authors,
        String publisher,
        String publishedDate,
        String description,
        Integer pageCount,
        List<String> categories,
        GoogleBookImageLinks imageLinks,
        List<GoogleBookIndustryIdentifier> industryIdentifiers
) {}

