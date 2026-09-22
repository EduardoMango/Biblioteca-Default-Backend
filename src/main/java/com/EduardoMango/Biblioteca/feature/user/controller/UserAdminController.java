package com.EduardoMango.Biblioteca.feature.user.controller;

import com.EduardoMango.Biblioteca.feature.user.dto.UserAdminResponse;
import com.EduardoMango.Biblioteca.feature.user.dto.UserRoleUpdateRequest;
import com.EduardoMango.Biblioteca.feature.user.dto.UserStatusUpdateRequest;
import com.EduardoMango.Biblioteca.feature.user.service.UserAdminService;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Administración de Usuarios", description = "Endpoints de gestión institucional para activar/desactivar cuentas de socios y modificar roles de acceso con reglas de autoprotección")
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @Operation(summary = "Modificar estado de cuenta del usuario",
            description = "Activa o desactiva una cuenta. Aplica reglas de negocio: el bibliotecario no puede desactivar su propia cuenta ni suspender usuarios con préstamos activos/vencidos. Sincroniza con Spring Security UserDetails.isEnabled(). Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de cuenta actualizado exitosamente", content = @Content(schema = @Schema(implementation = UserAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Autodesactivación prohibida o usuario con préstamos pendientes de devolución", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado para el publicId provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{publicId}/estado")
    @PreAuthorize("hasRole('ROLE_BIBLIOTECARIO') or hasRole('BIBLIOTECARIO')")
    public ResponseEntity<UserAdminResponse> updateUserStatus(
            @Parameter(description = "Identificador público UUID del usuario", example = "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d")
            @PathVariable UUID publicId,
            @Valid @RequestBody UserStatusUpdateRequest request,
            Authentication authentication) {
        UserAdminResponse response = userAdminService.updateUserStatus(publicId, request.activo(), authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Modificar rol de usuario",
            description = "Asigna un nuevo rol institucional (SOCIO o BIBLIOTECARIO). El usuario autenticado no puede rebajar ni modificar su propio rol. Requiere rol BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol actualizado exitosamente", content = @Content(schema = @Schema(implementation = UserAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Automodificación de rol prohibida o rol inexistente", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol BIBLIOTECARIO)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{publicId}/rol")
    @PreAuthorize("hasRole('ROLE_BIBLIOTECARIO') or hasRole('BIBLIOTECARIO')")
    public ResponseEntity<UserAdminResponse> updateUserRole(
            @Parameter(description = "Identificador público UUID del usuario", example = "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d")
            @PathVariable UUID publicId,
            @Valid @RequestBody UserRoleUpdateRequest request,
            Authentication authentication) {
        UserAdminResponse response = userAdminService.updateUserRole(publicId, request.nuevoRol(), authentication.getName());
        return ResponseEntity.ok(response);
    }
}
