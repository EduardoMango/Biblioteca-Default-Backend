package com.EduardoMango.Biblioteca.feature.loan.scheduler;

import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.infrastructure.email.model.EmailMessage;
import com.EduardoMango.Biblioteca.infrastructure.email.port.out.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanReminderScheduler {

    private final LoanRepository loanRepository;
    private final EmailSender emailSender;

    @Scheduled(cron = "${app.scheduler.loan-reminder.cron:0 0 8 * * *}")
    @Transactional(readOnly = true)
    public void sendDueSoonLoanReminders() {
        LocalDate targetDate = LocalDate.now().plusDays(2);
        log.info("Running scheduled loan reminder task for due date: {}", targetDate);

        List<Loan> dueLoans = loanRepository.findActiveLoansDueAt(LoanStatus.PRESTADO, targetDate);
        log.info("Found {} active loans due in 48 hours", dueLoans.size());

        for (Loan loan : dueLoans) {
            try {
                sendReminderEmail(loan);
            } catch (Exception e) {
                log.error("Error processing reminder for loan ID {}: {}", loan.getPublicId(), e.getMessage(), e);
            }
        }
    }

    private void sendReminderEmail(Loan loan) {
        String recipient = loan.getUsuario().getEmail();
        String userName = loan.getUsuario().getNombre() + " " + loan.getUsuario().getApellido();
        String bookTitle = loan.getLibro().getTitulo();
        String subject = "Recordatorio de Devolución - " + bookTitle;
        String htmlBody = String.format(
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body>
                    <h2>Recordatorio de Devolución de Préstamo</h2>
                    <p>Estimado/a <strong>%s</strong>,</p>
                    <p>Le recordamos que el préstamo del libro <strong>%s</strong> vencerá en 48 horas, el próximo <strong>%s</strong>.</p>
                    <p>Identificador de Préstamo: <strong>%s</strong></p>
                    <p>Por favor, asegúrese de devolver el ejemplar a tiempo en la biblioteca para evitar sanciones.</p>
                    <p>Saludos cordiales,<br>Biblioteca</p>
                </body>
                </html>
                """,
                userName,
                bookTitle,
                loan.getFechaDevolucionEsperada(),
                loan.getPublicId()
        );

        emailSender.send(new EmailMessage(recipient, subject, htmlBody));
    }
}

