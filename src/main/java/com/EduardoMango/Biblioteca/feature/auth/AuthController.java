package com.EduardoMango.Biblioteca.feature.auth;

import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.AuthResponse;
import com.EduardoMango.Biblioteca.feature.auth.dto.RefreshTokenRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.RegisterRequest;
import com.EduardoMango.Biblioteca.feature.user.dto.UserDTO;
import com.EduardoMango.Biblioteca.feature.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.EduardoMango.Biblioteca.feature.auth.dto.ForgotPasswordRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.MessageResponse;
import com.EduardoMango.Biblioteca.feature.auth.dto.ResetPasswordRequest;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final com.EduardoMango.Biblioteca.feature.auth.service.AccountVerificationService verificationService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse response = authService.login(authRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        UserDTO userDTO = userService.save(registerRequest);
        return new ResponseEntity<>(userDTO, HttpStatus.CREATED);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.processForgotPassword(request);
        return ResponseEntity.ok(new MessageResponse("Si el correo se encuentra registrado, se enviará un enlace de recuperación."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Contraseña restablecida exitosamente."));
    }

    @PostMapping("/confirm-account")
    public ResponseEntity<MessageResponse> confirmAccount(
            @org.springframework.web.bind.annotation.RequestParam(value = "token", required = false) String tokenParam,
            @RequestBody(required = false) com.EduardoMango.Biblioteca.feature.auth.dto.ConfirmAccountRequest body) {
        String token = (body != null && body.token() != null && !body.token().isBlank())
                ? body.token()
                : tokenParam;
        if (token == null || token.isBlank()) {
            throw new com.EduardoMango.Biblioteca.exception.BusinessRuleException("El token de confirmación es obligatorio");
        }
        verificationService.confirmAccount(token);
        return ResponseEntity.ok(new MessageResponse("Cuenta activada exitosamente"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody com.EduardoMango.Biblioteca.feature.auth.dto.ResendVerificationRequest request) {
        verificationService.resendVerification(request);
        return ResponseEntity.ok(new MessageResponse("Si la cuenta se encuentra registrada y pendiente de confirmación, se ha enviado un nuevo código."));
    }
}

