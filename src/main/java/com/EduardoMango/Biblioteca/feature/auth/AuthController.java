package com.EduardoMango.Biblioteca.feature.auth;

import com.EduardoMango.Biblioteca.feature.auth.dto.*;
import com.EduardoMango.Biblioteca.feature.user.dto.UserDTO;
import com.EduardoMango.Biblioteca.feature.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticación y Cuentas", description = "Endpoints públicos para registro de socios, inicio de sesión JWT, rotación de tokens, activación de cuenta y recuperación de contraseña")
@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final com.EduardoMango.Biblioteca.feature.auth.service.AccountVerificationService verificationService;

    @Operation(summary = "Iniciar sesión", description = "Autentica las credenciales del usuario (username y contraseña) y retorna un par de tokens JWT (Access Token de 15 min y Refresh Token de 7 días).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cuenta inactiva / no confirmada o solicitud inválida", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas (usuario o contraseña inválidos)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse response = authService.login(authRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Registrar nuevo socio", description = "Registra un nuevo usuario con rol SOCIO en estado inactivo y despacha un correo con token de activación (TTL 15 min).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente (pendiente de confirmación)", content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Error de validación en campos o correo/username ya en uso", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto por duplicidad de email o username", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        UserDTO userDTO = userService.save(registerRequest);
        return new ResponseEntity<>(userDTO, HttpStatus.CREATED);
    }

    @Operation(summary = "Refrescar token de acceso", description = "Renueva el par de tokens JWT utilizando un Refresh Token válido, implementando rotación para invalidar el token anterior.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados exitosamente", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Refresh token expirado, inválido o no coincidente", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Token no autorizado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Solicitar recuperación de contraseña", description = "Envía un correo con token temporal de reseteo si el email existe. Implementa protección anti-enumeración retornando siempre 200 OK.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensaje genérico de confirmación despachado", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Formato de email inválido", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.processForgotPassword(request);
        return ResponseEntity.ok(new MessageResponse("Si el correo se encuentra registrado, se enviará un enlace de recuperación."));
    }

    @Operation(summary = "Restablecer contraseña con token", description = "Permite cambiar la contraseña mediante el token de recuperación recibido por email, invalidándolo para usos posteriores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Token expirado, inválido o contraseña débil", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Contraseña restablecida exitosamente."));
    }

    @Operation(summary = "Confirmar y activar cuenta", description = "Activa la cuenta del usuario validando el token de verificación recibido por email (admitido tanto por query param ?token= como en el cuerpo JSON).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta activada exitosamente", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Token ausente, inválido o expirado (> 15 min)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/confirm-account")
    public ResponseEntity<MessageResponse> confirmAccount(
            @Parameter(description = "Token de verificación provisto por URL", example = "e2b7a9f1-4c8d-4a35-b210-9876543210ab")
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestBody(required = false) ConfirmAccountRequest body) {
        String token = (body != null && body.token() != null && !body.token().isBlank())
                ? body.token()
                : tokenParam;
        if (token == null || token.isBlank()) {
            throw new com.EduardoMango.Biblioteca.exception.BusinessRuleException("El token de confirmación es obligatorio");
        }
        verificationService.confirmAccount(token);
        return ResponseEntity.ok(new MessageResponse("Cuenta activada exitosamente"));
    }

    @Operation(summary = "Reenviar token de activación", description = "Genera y despacha un nuevo token de confirmación por correo si la cuenta se encuentra registrada y en estado pendiente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada (mensaje genérico anti-enumeración)", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Email con formato inválido o cuenta ya activada", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        verificationService.resendVerification(request);
        return ResponseEntity.ok(new MessageResponse("Si la cuenta se encuentra registrada y pendiente de confirmación, se ha enviado un nuevo código."));
    }
}
