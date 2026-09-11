package com.EduardoMango.Biblioteca.feature.auth;

import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.AuthResponse;
import com.EduardoMango.Biblioteca.feature.auth.domain.CredentialsEntity;
import com.EduardoMango.Biblioteca.feature.auth.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.security.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.auth.domain.PasswordResetToken;
import com.EduardoMango.Biblioteca.feature.auth.dto.ForgotPasswordRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.ResetPasswordRequest;
import com.EduardoMango.Biblioteca.feature.auth.repository.PasswordResetTokenRepository;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import com.EduardoMango.Biblioteca.feature.user.UserRepository;
import com.EduardoMango.Biblioteca.infrastructure.email.model.EmailMessage;
import com.EduardoMango.Biblioteca.infrastructure.email.port.out.EmailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CredentialsRepository credentialsRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse login(AuthRequest input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.username(),
                        input.password()
                )
        );

        CredentialsEntity credentials = credentialsRepository.findByUsername(input.username())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con username: " + input.username()));

        String accessToken = jwtService.generateToken(credentials);
        String refreshToken = jwtService.generateRefreshToken(credentials);

        credentials.setRefreshToken(refreshToken);
        credentialsRepository.save(credentials);

        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new IllegalArgumentException("El token de refresco tiene un formato inválido");
        }

        CredentialsEntity user = credentialsRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado para el refresh token"));

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new IllegalArgumentException("El token de refresco no coincide con el registrado en el sistema");
        }

        if (!jwtService.validateRefreshToken(refreshToken, user)) {
            throw new IllegalArgumentException("El token de refresco ha expirado o es inválido");
        }

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        user.setRefreshToken(newRefreshToken);
        credentialsRepository.save(user);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void processForgotPassword(ForgotPasswordRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            return;
        }

        userRepository.findByEmail(request.email().trim().toLowerCase())
                .ifPresent(user -> {
                    String tokenValue = UUID.randomUUID().toString();
                    PasswordResetToken resetToken = PasswordResetToken.builder()
                            .token(tokenValue)
                            .user(user)
                            .expiryDate(LocalDateTime.now().plusMinutes(15))
                            .used(false)
                            .build();
                    passwordResetTokenRepository.save(resetToken);

                    String subject = "Restablecimiento de Contraseña - Biblioteca";
                    String htmlBody = String.format(
                            """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                            </head>
                            <body>
                                <h2>Restablecimiento de Contraseña</h2>
                                <p>Hola <strong>%s</strong>,</p>
                                <p>Hemos recibido una solicitud para restablecer la contraseña de tu cuenta.</p>
                                <p>Para completar el proceso, utiliza el siguiente token de verificación (válido por 15 minutos):</p>
                                <p style="font-size: 18px; font-weight: bold; color: #2C3E50;">%s</p>
                                <p>Si no has solicitado este restablecimiento, puedes ignorar este mensaje de forma segura.</p>
                                <p>Saludos cordiales,<br>Biblioteca</p>
                            </body>
                            </html>
                            """,
                            user.getNombre(),
                            tokenValue
                    );

                    emailSender.send(new EmailMessage(user.getEmail(), subject, htmlBody));
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BusinessRuleException("El token es inválido, ha expirado o ya fue utilizado"));

        if (token.isUsed() || token.isExpired()) {
            throw new BusinessRuleException("El token es inválido, ha expirado o ya fue utilizado");
        }

        UserEntity user = token.getUser();
        CredentialsEntity credentials = credentialsRepository.findByUsuario(user)
                .orElseThrow(() -> new ResourceNotFoundException("Credenciales no encontradas para el usuario"));

        credentials.setPassword(passwordEncoder.encode(request.newPassword()));
        credentialsRepository.save(credentials);

        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findByUserAndUsedFalse(user);
        for (PasswordResetToken activeToken : activeTokens) {
            activeToken.setUsed(true);
        }
        passwordResetTokenRepository.saveAll(activeTokens);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
    }
}

