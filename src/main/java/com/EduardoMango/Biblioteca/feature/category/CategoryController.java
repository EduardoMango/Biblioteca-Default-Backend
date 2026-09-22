package com.EduardoMango.Biblioteca.feature.category;

import com.EduardoMango.Biblioteca.feature.category.dto.CategoryRequest;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryResponse;
import com.EduardoMango.Biblioteca.feature.category.service.CategoryService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Gestión de Categorías", description = "Endpoints para la administración taxonómica de géneros de libros con validación SPI desacoplada de eliminación")
@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Crear nueva categoría", description = "Registra una nueva categoría en el sistema. El nombre debe ser único independientemente de mayúsculas/minúsculas. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente", content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una categoría registrada con el mismo nombre", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar categorías paginadas", description = "Recupera las categorías de libros ordenadas con soporte de paginación. Accesible para SOCIO y BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de categorías obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<Page<CategoryResponse>> getCategories(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(categoryService.getCategories(pageable));
    }

    @Operation(summary = "Obtener categoría por ID público", description = "Recupera los datos de una categoría mediante su identificador UUID. Accesible para SOCIO y BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría encontrada", content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada para el UUID especificado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<CategoryResponse> getCategoryByPublicId(
            @Parameter(description = "Identificador público UUID de la categoría", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(categoryService.getCategoryByPublicId(publicId));
    }

    @Operation(summary = "Actualizar categoría", description = "Actualiza el nombre y/o descripción de una categoría existente. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada exitosamente", content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe otra categoría con el nuevo nombre ingresado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{publicId}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @Parameter(description = "Identificador público UUID de la categoría", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID publicId,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(publicId, request));
    }

    @Operation(summary = "Eliminar categoría", description = "Elimina una categoría si no posee ningún libro vinculado (validado mediante SPI CategoryDeletionValidator). Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoría eliminada exitosamente"),
            @ApiResponse(responseCode = "400", description = "No se puede eliminar la categoría porque posee libros asociados", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Identificador público UUID de la categoría", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @PathVariable UUID publicId) {
        categoryService.deleteCategory(publicId);
        return ResponseEntity.noContent().build();
    }
}
