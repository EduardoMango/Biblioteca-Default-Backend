package com.EduardoMango.Biblioteca.feature.loan.listener;

import com.EduardoMango.Biblioteca.feature.loan.event.LoanCreatedEvent;
import com.EduardoMango.Biblioteca.infrastructure.email.model.EmailMessage;
import com.EduardoMango.Biblioteca.infrastructure.email.port.out.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanNotificationListener {

    private final EmailSender emailSender;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLoanCreated(LoanCreatedEvent event) {
        log.info("Processing loan created notification for loan: {}", event.loanPublicId());

        String subject = "Comprobante de Préstamo - " + event.bookTitle();
        String htmlBody = String.format(
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body>
                    <h2>Comprobante de Registro de Préstamo</h2>
                    <p>Estimado/a <strong>%s</strong>,</p>
                    <p>Le informamos que se ha registrado un nuevo préstamo a su nombre con los siguientes detalles:</p>
                    <ul>
                        <li><strong>Identificador de Préstamo:</strong> %s</li>
                        <li><strong>Libro:</strong> %s</li>
                        <li><strong>Fecha de Emisión:</strong> %s</li>
                        <li><strong>Fecha Límite de Devolución:</strong> %s</li>
                    </ul>
                    <p>Por favor, recuerde devolver el ejemplar antes de la fecha límite para evitar penalizaciones.</p>
                    <p>Saludos cordiales,<br>Biblioteca</p>
                </body>
                </html>
                """,
                event.userName(),
                event.loanPublicId(),
                event.bookTitle(),
                event.fechaPrestamo(),
                event.fechaDevolucionEsperada()
        );

        emailSender.send(new EmailMessage(event.userEmail(), subject, htmlBody));
    }
}

