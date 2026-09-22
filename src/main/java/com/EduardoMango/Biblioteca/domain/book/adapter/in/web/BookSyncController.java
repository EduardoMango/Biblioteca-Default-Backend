package com.EduardoMango.Biblioteca.domain.book.adapter.in.web;

import com.EduardoMango.Biblioteca.domain.book.service.BookSyncService;
import com.EduardoMango.Biblioteca.feature.book.dto.BookResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Tag(name = "Sincronización de Libros", description = "Endpoints de enriquecimiento y sincronización de metadatos no destructiva con proveedores externos (Google Books API)")
@RestController
@RequestMapping({"/api/v1/books", "/api/libros"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BookSyncController {

    private final BookSyncService bookSyncService;

    @Operation(summary = "Sincronizar libro con Google Books API",
            description = "Actualiza metadatos (descripción, editorial, portada) de forma no destructiva sin modificar el inventario físico ni el historial de préstamos. Si force=true, sobrescribe campos existentes; de lo contrario solo completa campos nulos/vacíos. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro sincronizado exitosamente", content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "El libro no posee ISBN para sincronizar o solicitud inválida", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado en el catálogo local o no hallado en Google Books", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{identifier}/sync-google-books")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public Mono<ResponseEntity<BookResponse>> syncGoogleBooks(
            @Parameter(description = "Identificador público UUID o código ISBN del libro a sincronizar", example = "9780307474728")
            @PathVariable String identifier,
            @Parameter(description = "Si es true, sobrescribe campos locales con los datos remotos de Google Books", example = "false")
            @RequestParam(name = "force", defaultValue = "false") boolean force) {
        return bookSyncService.syncBook(identifier, force)
                .map(ResponseEntity::ok);
    }
}
