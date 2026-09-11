package com.EduardoMango.Biblioteca.feature.auth.listener;

import com.EduardoMango.Biblioteca.feature.auth.service.AccountVerificationService;
import com.EduardoMango.Biblioteca.feature.user.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountVerificationListener {

    private final AccountVerificationService verificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Processing UserRegisteredEvent for user: {}", event.email());
        verificationService.createAndSendVerificationToken(event.userPublicId(), event.email(), event.nombre());
    }
}

