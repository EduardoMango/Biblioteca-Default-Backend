package com.EduardoMango.Biblioteca.feature.loan;

import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.category.Category;
import com.EduardoMango.Biblioteca.feature.category.CategoryRepository;
import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.feature.loan.scheduler.LoanReminderScheduler;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import com.EduardoMango.Biblioteca.feature.user.UserRepository;
import com.EduardoMango.Biblioteca.infrastructure.email.model.EmailMessage;
import com.EduardoMango.Biblioteca.infrastructure.email.port.out.EmailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class LoanReminderSchedulerIntegrationTest {

    @Autowired
    private LoanReminderScheduler scheduler;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    @DisplayName("Escenario 1 y 2 BDD: Notifica préstamos activos en 48hs e ignora devueltos o fuera de rango")
    void testSchedulerProcessesOnlyActiveLoansDueIn48Hours() {
        reset(emailSender);

        Category cat = categoryRepository.save(Category.builder()
                .nombre("SchedCat-" + UUID.randomUUID())
                .descripcion("Desc")
                .build());

        Book book = bookRepository.save(Book.builder()
                .titulo("Clean Code")
                .isbn("ISBN-" + UUID.randomUUID().toString().substring(0, 10))
                .stockTotal(10)
                .stockDisponible(10)
                .categoria(cat)
                .build());

        UserEntity user1 = userRepository.save(UserEntity.builder()
                .nombre("Carlos")
                .apellido("Santana")
                .email("carlos.due@test.com")
                .dni(UUID.randomUUID().toString().substring(0, 8))
                .rol("SOCIO")
                .activo(true)
                .build());

        UserEntity user2 = userRepository.save(UserEntity.builder()
                .nombre("Maria")
                .apellido("Gomez")
                .email("maria.devuelto@test.com")
                .dni(UUID.randomUUID().toString().substring(0, 8))
                .rol("SOCIO")
                .activo(true)
                .build());

        UserEntity user3 = userRepository.save(UserEntity.builder()
                .nombre("Luis")
                .apellido("Perez")
                .email("luis.fuerarango@test.com")
                .dni(UUID.randomUUID().toString().substring(0, 8))
                .rol("SOCIO")
                .activo(true)
                .build());

        LocalDate in48Hours = LocalDate.now().plusDays(2);

        // Préstamo 1: ACTIVO, vence en 48hs -> DEBE ser notificado
        Loan loanActivo = loanRepository.save(Loan.builder()
                .usuario(user1)
                .libro(book)
                .fechaPrestamo(LocalDate.now().minusDays(12))
                .fechaDevolucionEsperada(in48Hours)
                .estado(LoanStatus.PRESTADO)
                .build());

        // Préstamo 2: DEVUELTO, vence en 48hs -> NO debe ser notificado
        Loan loanDevuelto = loanRepository.save(Loan.builder()
                .usuario(user2)
                .libro(book)
                .fechaPrestamo(LocalDate.now().minusDays(12))
                .fechaDevolucionEsperada(in48Hours)
                .fechaDevolucionEfectiva(LocalDate.now())
                .estado(LoanStatus.DEVUELTO)
                .build());

        // Préstamo 3: ACTIVO, vence en 5 días -> NO debe ser notificado
        Loan loanLejano = loanRepository.save(Loan.builder()
                .usuario(user3)
                .libro(book)
                .fechaPrestamo(LocalDate.now().minusDays(9))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(5))
                .estado(LoanStatus.PRESTADO)
                .build());

        // Ejecución del scheduler
        scheduler.sendDueSoonLoanReminders();

        // Verificaciones
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, times(1)).send(captor.capture());

        EmailMessage message = captor.getValue();
        assertThat(message.to()).isEqualTo("carlos.due@test.com");
        assertThat(message.subject()).contains("Clean Code");
        assertThat(message.body()).contains("Carlos Santana");
        assertThat(message.body()).contains("Clean Code");
        assertThat(message.body()).contains(in48Hours.toString());
    }
}

