package com.EduardoMango.Biblioteca.feature.loan;

import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanCreateRequest;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanResponse;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanSupervisionResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Préstamos y Devoluciones", description = "Endpoints para la gestión transaccional del ciclo de vida de préstamos, control atómico de stock, penalizaciones por mora y supervisión bibliotecaria")
@RestController
@RequestMapping("/api/prestamos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;

    @Operation(summary = "Solicitar un nuevo préstamo",
            description = "Registra un préstamo para el usuario autenticado (o para un socio indicado si quien ejecuta es bibliotecario). Verifica que el libro tenga stock disponible (> 0), que la cuenta del socio esté activa y que no retenga libros vencidos en mora. Reduce el stockDisponible en 1.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Préstamo concedido exitosamente", content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Sin stock disponible, socio con mora activa o cuenta inactiva", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Libro o usuario no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LoanResponse> createLoan(
            @Valid @RequestBody LoanCreateRequest request,
            Authentication authentication) {
        LoanResponse response = loanService.createLoan(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Registrar devolución física del libro",
            description = "Asienta la devolución de un préstamo. Si la fecha actual supera la fecha de devolución esperada, transiciona a estado CON_RETRASO; de lo contrario a DEVUELTO. Incrementa el stockDisponible en 1 y levanta la restricción de mora. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolución registrada exitosamente", content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "400", description = "El préstamo ya se encuentra devuelto previamente", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Préstamo no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{publicId}/devolucion")
    @PreAuthorize("hasRole('ROLE_BIBLIOTECARIO') or hasRole('BIBLIOTECARIO')")
    public ResponseEntity<LoanResponse> returnLoan(
            @Parameter(description = "Identificador público UUID del préstamo a devolver", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID publicId) {
        LoanResponse response = loanService.returnLoan(publicId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Supervisar y auditar préstamos",
            description = "Panel de supervisión con filtros combinables por estado del préstamo, solo préstamos atrasados y por socio. Soporta ordenamiento y paginación. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de supervisión recuperada exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/supervision")
    @PreAuthorize("hasRole('ROLE_BIBLIOTECARIO') or hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Page<LoanSupervisionResponse>> getLoansForSupervision(
            @Parameter(description = "Filtrar por estado del préstamo (PRESTADO, DEVUELTO, CON_RETRASO)", example = "PRESTADO")
            @RequestParam(required = false) LoanStatus estado,
            @Parameter(description = "Si es true, filtra exclusivamente préstamos cuya fecha límite expiró sin haberse devuelto", example = "true")
            @RequestParam(required = false) Boolean soloAtrasados,
            @Parameter(description = "Filtrar por identificador público UUID de un socio específico", example = "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d")
            @RequestParam(required = false) UUID usuarioPublicId,
            @ParameterObject
            @PageableDefault(size = 10, sort = "fechaDevolucionEsperada", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<LoanSupervisionResponse> page = loanService.getLoansForSupervision(estado, soloAtrasados, usuarioPublicId, pageable);
        return ResponseEntity.ok(page);
    }
}
