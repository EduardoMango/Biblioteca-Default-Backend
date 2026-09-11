package com.EduardoMango.Biblioteca.feature.loan;

import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.category.Category;
import com.EduardoMango.Biblioteca.feature.category.CategoryRepository;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanCreateRequest;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class LoanNotificationIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    @DisplayName("Escenario 1: Envío automático de correo tras registrar préstamo con éxito")
    void testEnvioAutomaticoCorreoTrasPrestamo() {
        Category cat = categoryRepository.save(Category.builder()
                .nombre("NotifCat-" + UUID.randomUUID())
                .descripcion("Cat desc")
                .build());

        Book book = bookRepository.save(Book.builder()
                .titulo("Libro Notificaciones")
                .isbn("ISBN-" + UUID.randomUUID().toString().substring(0, 10))
                .stockTotal(5)
                .stockDisponible(5)
                .categoria(cat)
                .build());

        UserEntity user = userRepository.save(UserEntity.builder()
                .nombre("Pedro")
                .apellido("Gomez")
                .email("pedro.notif@test.com")
                .dni(UUID.randomUUID().toString().substring(0, 8))
                .rol("SOCIO")
                .activo(true)
                .build());

        LoanCreateRequest request = new LoanCreateRequest(book.getIsbn(), null);

        loanService.createLoan(request, user.getEmail());

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, timeout(3000).times(1)).send(captor.capture());

        EmailMessage sent = captor.getValue();
        assertThat(sent.to()).isEqualTo("pedro.notif@test.com");
        assertThat(sent.subject()).contains("Libro Notificaciones");
        assertThat(sent.body()).contains("Pedro Gomez");
        assertThat(sent.body()).contains("Libro Notificaciones");
    }

    @Test
    @DisplayName("Escenario 2: Falla en creación de préstamo cancela o no envía la notificación")
    void testFallaCancelaNotificacion() {
        reset(emailSender);

        Category cat = categoryRepository.save(Category.builder()
                .nombre("FailCat-" + UUID.randomUUID())
                .descripcion("Cat desc")
                .build());

        Book bookAgotado = bookRepository.save(Book.builder()
                .titulo("Libro Agotado")
                .isbn("ISBN-" + UUID.randomUUID().toString().substring(0, 10))
                .stockTotal(0)
                .stockDisponible(0)
                .categoria(cat)
                .build());

        UserEntity user = userRepository.save(UserEntity.builder()
                .nombre("Ana")
                .apellido("Lopez")
                .email("ana.notif@test.com")
                .dni(UUID.randomUUID().toString().substring(0, 8))
                .rol("SOCIO")
                .activo(true)
                .build());

        LoanCreateRequest request = new LoanCreateRequest(bookAgotado.getIsbn(), null);

        assertThatThrownBy(() -> loanService.createLoan(request, user.getEmail()));

        verify(emailSender, after(500).never()).send(any());
    }
}

