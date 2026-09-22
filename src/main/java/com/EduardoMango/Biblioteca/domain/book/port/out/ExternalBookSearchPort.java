package com.EduardoMango.Biblioteca.domain.book.port.out;

import com.EduardoMango.Biblioteca.domain.book.dto.ExternalBookDto;
import reactor.core.publisher.Mono;

public interface ExternalBookSearchPort {
    Mono<ExternalBookDto> findByIsbn(String isbn);
}

