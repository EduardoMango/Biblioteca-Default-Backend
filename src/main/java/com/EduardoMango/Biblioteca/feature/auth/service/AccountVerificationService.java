package com.EduardoMango.Biblioteca.feature.auth.service;

import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.feature.auth.domain.AccountVerificationToken;
import com.EduardoMango.Biblioteca.feature.auth.domain.CredentialsEntity;
import com.EduardoMango.Biblioteca.feature.auth.dto.ResendVerificationRequest;
import com.EduardoMango.Biblioteca.feature.auth.repository.AccountVerificationTokenRepository;
import com.EduardoMango.Biblioteca.feature.auth.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import com.EduardoMango.Biblioteca.feature.user.UserRepository;
import com.EduardoMango.Biblioteca.infrastructure.email.model.EmailMessage;
import com.EduardoMango.Biblioteca.infrastructure.email.port.out.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountVerificationService {

    private final AccountVerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final EmailSender emailSender;

    @Transactional
    public void createAndSendVerificationToken(UUID userPublicId, String email, String nombre) {
        userRepository.findByPublicId(userPublicId).ifPresent(user -> {
            String tokenValue = UUID.randomUUID().toString();
            AccountVerificationToken token = AccountVerificationToken.builder()
                    .token(tokenValue)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(15))
                    .used(false)
                    .build();
            verificationTokenRepository.save(token);

            sendVerificationEmail(email, nombre, tokenValue);
        });
    }

    @Transactional
    public void confirmAccount(String tokenValue) {
        AccountVerificationToken token = verificationTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessRuleException("El token de confirmación es inválido, ha expirado o ya fue utilizado"));

        if (token.isUsed() || token.isExpired()) {
            throw new BusinessRuleException("El token de confirmación es inválido, ha expirado o ya fue utilizado");
        }

        UserEntity user = token.getUser();
        user.setEnabled(true);
        user.setActivo(true);
        userRepository.save(user);

        credentialsRepository.findByUsuario(user).ifPresent(creds -> {
            creds.setEnabled(true);
            credentialsRepository.save(creds);
        });

        List<AccountVerificationToken> pendingTokens = verificationTokenRepository.findByUserAndUsedFalse(user);
        for (AccountVerificationToken pendingToken : pendingTokens) {
            pendingToken.setUsed(true);
        }
        verificationTokenRepository.saveAll(pendingTokens);

        token.setUsed(true);
        verificationTokenRepository.save(token);
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            return;
        }

        userRepository.findByEmail(request.email().trim().toLowerCase()).ifPresent(user -> {
            boolean isEnabled = credentialsRepository.findByUsuario(user)
                    .map(CredentialsEntity::isEnabled)
                    .orElse(false);

            if (isEnabled) {
                return;
            }

            List<AccountVerificationToken> previousTokens = verificationTokenRepository.findByUserAndUsedFalse(user);
            for (AccountVerificationToken previousToken : previousTokens) {
                previousToken.setUsed(true);
            }
            verificationTokenRepository.saveAll(previousTokens);

            String newTokenValue = UUID.randomUUID().toString();
            AccountVerificationToken newToken = AccountVerificationToken.builder()
                    .token(newTokenValue)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(15))
                    .used(false)
                    .build();
            verificationTokenRepository.save(newToken);

            sendVerificationEmail(user.getEmail(), user.getNombre(), newTokenValue);
        });
    }

    private void sendVerificationEmail(String email, String nombre, String tokenValue) {
        String subject = "Verificación y Confirmación de Cuenta - Biblioteca";
        String htmlBody = String.format(
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body>
                    <h2>Activación de Cuenta en Biblioteca</h2>
                    <p>Estimado/a <strong>%s</strong>,</p>
                    <p>Gracias por registrarte en nuestra Biblioteca. Para poder iniciar sesión, activa tu cuenta con el siguiente código/token de verificación (válido por 15 minutos):</p>
                    <p style="font-size: 18px; font-weight: bold; color: #1E88E5;">%s</p>
                    <p>Si no te registraste en nuestro sistema, por favor desestima este correo.</p>
                    <p>Saludos cordiales,<br>Biblioteca</p>
                </body>
                </html>
                """,
                nombre,
                tokenValue
        );

        emailSender.send(new EmailMessage(email, subject, htmlBody));
    }
}

