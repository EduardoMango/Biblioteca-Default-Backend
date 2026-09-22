package com.EduardoMango.Biblioteca.feature.book;

import com.EduardoMango.Biblioteca.feature.book.dto.*;
import com.EduardoMango.Biblioteca.feature.book.service.BookImportService;
import com.EduardoMango.Biblioteca.feature.book.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Catálogo de Libros y Stock", description = "Endpoints para la gestión integral del catálogo bibliográfico, inventario físico, búsqueda multicriterio e importación desde Google Books API")
@RestController
@RequestMapping({"/api/v1/books", "/api/libros"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BookController {

    private final BookService bookService;
    private final BookImportService bookImportService;

    @Operation(summary = "Importar libro desde Google Books API", description = "Consulta metadatos externamente por ISBN y persiste automáticamente el libro, sus autores y categoría sugerida. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Libro importado y persistido exitosamente", content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No se encontró el libro en la API de Google Books para el ISBN especificado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "El libro ya se encuentra registrado en el catálogo local", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/import")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public Mono<ResponseEntity<BookResponse>> importBook(@Valid @RequestBody ImportBookRequest request) {
        return bookImportService.importBook(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Previsualizar borrador de libro externo", description = "Consulta Google Books API y retorna un borrador no persistido indicando si ya existe en el catálogo local. Accesible para SOCIO y BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Borrador obtenido exitosamente", content = @Content(schema = @Schema(implementation = ExternalBookPreviewDto.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "ISBN no encontrado en el proveedor externo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/external/isbn/{isbn}")
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public Mono<ResponseEntity<ExternalBookPreviewDto>> previewExternalBook(
            @Parameter(description = "Código ISBN de 10 o 13 dígitos", example = "9780307474728")
            @PathVariable String isbn) {
        return bookImportService.previewExternalBook(isbn)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Crear libro manualmente", description = "Registra un libro con stock inicial, asignando categoría y autores existentes. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Libro registrado exitosamente", content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o categoría/autor inexistente", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un libro registrado con ese ISBN", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookCreateRequest request) {
        BookResponse response = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Buscar libros con filtros combinados", description = "Búsqueda dinámica multicriterio sobre el catálogo (por título, ISBN, categoría, autor y solo disponibles) con paginación y ordenamiento. Accesible para SOCIO y BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de libros obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<Page<BookResponse>> searchBooks(
            @Parameter(description = "Filtrar por coincidencia parcial en el título", example = "Cien años")
            @RequestParam(required = false) String titulo,
            @Parameter(description = "Filtrar por código ISBN exacto o parcial", example = "9780307474728")
            @RequestParam(required = false) String isbn,
            @Parameter(description = "Filtrar por identificador público de categoría", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @RequestParam(required = false) UUID categoriaPublicId,
            @Parameter(description = "Filtrar por identificador público del autor", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestParam(required = false) UUID autorPublicId,
            @Parameter(description = "Si es true, incluye únicamente libros con stock disponible mayor a cero", example = "true")
            @RequestParam(required = false) Boolean soloDisponibles,
            @ParameterObject
            @PageableDefault(page = 0, size = 10, sort = "titulo", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(bookService.searchBooks(titulo, isbn, categoriaPublicId, autorPublicId, soloDisponibles, pageable));
    }

    @Operation(summary = "Obtener libro por ISBN", description = "Recupera la información completa de un libro por su ISBN. Accesible para SOCIO y BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro encontrado", content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado para el ISBN especificado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{isbn}")
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> getBookByIsbn(
            @Parameter(description = "Código ISBN del libro", example = "9780307474728")
            @PathVariable String isbn) {
        return ResponseEntity.ok(bookService.getBookByIsbn(isbn));
    }

    @Operation(summary = "Actualizar libro completo", description = "Actualiza metadatos y stock total de un libro garantizando la consistencia con ejemplares prestados. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro actualizado exitosamente", content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "Inconsistencia de stock o datos inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{isbn}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> updateBook(
            @Parameter(description = "Código ISBN del libro", example = "9780307474728")
            @PathVariable String isbn,
            @Valid @RequestBody BookUpdateRequest request) {
        return ResponseEntity.ok(bookService.updateBook(isbn, request));
    }

    @Operation(summary = "Ajustar stock físico del libro", description = "Modifica la cantidad total de ejemplares físicos. El nuevo total no puede ser menor a los préstamos actualmente activos. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock ajustado exitosamente", content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "El nuevo stock es menor a los préstamos en curso", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{isbn}/stock")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> adjustStock(
            @Parameter(description = "Código ISBN del libro", example = "9780307474728")
            @PathVariable String isbn,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(bookService.adjustStock(isbn, request));
    }

    @Operation(summary = "Actualizar portada del libro", description = "Actualiza la URL de la imagen de portada del libro. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portada actualizada exitosamente", content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "URL inválida o excedida en longitud", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{isbn}/portada")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> updateCover(
            @Parameter(description = "Código ISBN del libro", example = "9780307474728")
            @PathVariable String isbn,
            @Valid @RequestBody BookCoverUpdateRequest request) {
        return ResponseEntity.ok(bookService.updateCover(isbn, request.urlPortada()));
    }

    @Operation(summary = "Modificación parcial del libro", description = "Actualiza atributos puntuales del libro sin tocar el stock físico. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro modificado exitosamente", content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{isbn}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<BookResponse> patchBook(
            @Parameter(description = "Código ISBN del libro", example = "9780307474728")
            @PathVariable String isbn,
            @Valid @RequestBody BookPatchRequest request) {
        return ResponseEntity.ok(bookService.patchBook(isbn, request));
    }
}
