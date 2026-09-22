package com.EduardoMango.Biblioteca.feature.user.controller;

import com.EduardoMango.Biblioteca.feature.user.dto.LoanHistoryResponse;
import com.EduardoMango.Biblioteca.feature.user.dto.UserProfileResponse;
import com.EduardoMango.Biblioteca.feature.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Perfil del Usuario", description = "Endpoints personales para consulta de perfil e historial de préstamos del usuario autenticado (principio self-only)")
@RestController
@RequestMapping("/api/usuarios/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "Obtener perfil del usuario autenticado",
            description = "Retorna los datos personales (nombre, apellido, email, rol) y los últimos préstamos del usuario identificado por el token JWT actual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil recuperado exitosamente", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        UserProfileResponse response = userProfileService.getUserProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar historial personal de préstamos",
            description = "Retorna el listado paginado de todos los préstamos solicitados por el usuario autenticado, ordenados descendentemente por fecha de emisión.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de préstamos obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/prestamos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LoanHistoryResponse>> getMyLoans(
            Authentication authentication,
            @ParameterObject
            @PageableDefault(size = 10, sort = "fechaPrestamo", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LoanHistoryResponse> response = userProfileService.getUserLoans(authentication.getName(), pageable);
        return ResponseEntity.ok(response);
    }
}
