package com.EduardoMango.Biblioteca.infrastructure.googlebooks.adapter.out;

import com.EduardoMango.Biblioteca.domain.book.dto.ExternalBookDto;
import com.EduardoMango.Biblioteca.domain.book.port.out.ExternalBookSearchPort;
import com.EduardoMango.Biblioteca.infrastructure.googlebooks.dto.GoogleBookIndustryIdentifier;
import com.EduardoMango.Biblioteca.infrastructure.googlebooks.dto.GoogleBooksResponse;
import com.EduardoMango.Biblioteca.infrastructure.googlebooks.dto.GoogleBookVolumeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class GoogleBooksWebClientAdapter implements ExternalBookSearchPort {

    private final WebClient webClient;
    private final String apiKey;

    @org.springframework.beans.factory.annotation.Autowired
    public GoogleBooksWebClientAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${google.books.api.base-url:https://www.googleapis.com/books/v1}") String baseUrl,
            @Value("${google.books.api.key:}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    public GoogleBooksWebClientAdapter(WebClient webClient, String apiKey) {
        this.webClient = webClient;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    @Override
    public Mono<ExternalBookDto> findByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Mono.empty();
        }

        String cleanIsbn = isbn.trim();

        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/volumes")
                            .queryParam("q", "isbn:" + cleanIsbn);
                    if (!apiKey.isBlank()) {
                        builder.queryParam("key", apiKey);
                    }
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(GoogleBooksResponse.class)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                .flatMap(response -> {
                    if (response == null || response.items() == null || response.items().isEmpty()) {
                        log.info("No external book found in Google Books for ISBN: {}", cleanIsbn);
                        return Mono.empty();
                    }

                    GoogleBookVolumeInfo volumeInfo = response.items().get(0).volumeInfo();
                    if (volumeInfo == null) {
                        return Mono.empty();
                    }

                    return Mono.just(mapToExternalBookDto(cleanIsbn, volumeInfo));
                });
    }

    private ExternalBookDto mapToExternalBookDto(String fallbackIsbn, GoogleBookVolumeInfo volumeInfo) {
        String resolvedIsbn = resolveIsbn(volumeInfo, fallbackIsbn);
        String title = volumeInfo.title() != null ? volumeInfo.title() : "Sin título";
        List<String> authors = volumeInfo.authors() != null ? volumeInfo.authors() : Collections.emptyList();
        String publisher = volumeInfo.publisher();
        String description = volumeInfo.description();
        String coverUrl = resolveCoverUrl(volumeInfo);
        List<String> categories = volumeInfo.categories() != null ? volumeInfo.categories() : Collections.emptyList();
        Integer pageCount = volumeInfo.pageCount();
        String publishedDate = volumeInfo.publishedDate();

        return new ExternalBookDto(
                resolvedIsbn,
                title,
                authors,
                publisher,
                description,
                coverUrl,
                categories,
                pageCount,
                publishedDate
        );
    }

    private String resolveIsbn(GoogleBookVolumeInfo volumeInfo, String fallbackIsbn) {
        if (volumeInfo.industryIdentifiers() != null && !volumeInfo.industryIdentifiers().isEmpty()) {
            for (GoogleBookIndustryIdentifier id : volumeInfo.industryIdentifiers()) {
                if ("ISBN_13".equalsIgnoreCase(id.type())) {
                    return id.identifier();
                }
            }
            for (GoogleBookIndustryIdentifier id : volumeInfo.industryIdentifiers()) {
                if ("ISBN_10".equalsIgnoreCase(id.type())) {
                    return id.identifier();
                }
            }
        }
        return fallbackIsbn;
    }

    private String resolveCoverUrl(GoogleBookVolumeInfo volumeInfo) {
        if (volumeInfo.imageLinks() == null) {
            return null;
        }
        String cover = volumeInfo.imageLinks().thumbnail();
        if (cover == null || cover.isBlank()) {
            cover = volumeInfo.imageLinks().smallThumbnail();
        }
        if (cover != null && cover.startsWith("http://")) {
            cover = cover.replace("http://", "https://");
        }
        return cover;
    }
}
