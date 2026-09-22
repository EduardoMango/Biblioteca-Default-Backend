package com.EduardoMango.Biblioteca.feature.author;

import com.EduardoMango.Biblioteca.feature.author.dto.AuthorRequest;
import com.EduardoMango.Biblioteca.feature.author.dto.AuthorResponse;
import com.EduardoMango.Biblioteca.feature.author.service.AuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Gestión de Autores", description = "Endpoints para la administración del catálogo de autores, altas, búsquedas y control de borrado desacoplado (SPI)")
@RestController
@RequestMapping("/api/autores")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuthorController {

    private final AuthorService authorService;

    @Operation(summary = "Crear nuevo autor", description = "Registra un autor en el catálogo. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Autor creado exitosamente", content = @Content(schema = @Schema(implementation = AuthorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un autor con el mismo nombre y apellido", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<AuthorResponse> createAuthor(@Valid @RequestBody AuthorRequest request) {
        AuthorResponse response = authorService.createAuthor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Buscar y listar autores", description = "Obtiene la lista de autores con filtros opcionales por texto en nombre/apellido y por nacionalidad. Accesible para SOCIO y BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de autores recuperado exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuthorResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<List<AuthorResponse>> getAuthors(
            @Parameter(description = "Texto parcial para buscar por nombre o apellido", example = "García")
            @RequestParam(required = false) String q,
            @Parameter(description = "Filtro por nacionalidad exacta o parcial", example = "Colombiana")
            @RequestParam(required = false) String nacionalidad) {
        return ResponseEntity.ok(authorService.getAuthors(q, nacionalidad));
    }

    @Operation(summary = "Obtener autor por ID público", description = "Consulta los datos de un autor mediante su UUID público. Accesible para SOCIO y BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autor encontrado", content = @Content(schema = @Schema(implementation = AuthorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Autor no encontrado para el publicId especificado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<AuthorResponse> getAuthorByPublicId(
            @Parameter(description = "Identificador público UUID del autor", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(authorService.getAuthorByPublicId(publicId));
    }

    @Operation(summary = "Actualizar autor", description = "Modifica los datos de un autor existente. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autor actualizado exitosamente", content = @Content(schema = @Schema(implementation = AuthorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Autor no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto por duplicidad de nombre y apellido con otro autor", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{publicId}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<AuthorResponse> updateAuthor(
            @Parameter(description = "Identificador público UUID del autor a actualizar", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID publicId,
            @Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.ok(authorService.updateAuthor(publicId, request));
    }

    @Operation(summary = "Eliminar autor", description = "Elimina un autor del catálogo si no posee libros asociados (validado mediante SPI AuthorDeletionValidator). Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Autor eliminado exitosamente"),
            @ApiResponse(responseCode = "400", description = "No se puede eliminar el autor porque tiene libros asociados", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Autor no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> deleteAuthor(
            @Parameter(description = "Identificador público UUID del autor a eliminar", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID publicId) {
        authorService.deleteAuthor(publicId);
        return ResponseEntity.noContent().build();
    }
}
