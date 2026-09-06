package com.EduardoMango.Biblioteca.feature.loan.controller;

import com.EduardoMango.Biblioteca.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.book.domain.Book;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.category.domain.Category;
import com.EduardoMango.Biblioteca.feature.category.repository.CategoryRepository;
import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.model.entity.CredentialsEntity;
import com.EduardoMango.Biblioteca.model.entity.UserEntity;
import com.EduardoMango.Biblioteca.repository.CredentialsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class LoanReturnControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private String bibliotecarioToken;
    private String socioToken;
    private UserEntity socio;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Login bibliotecario
        AuthRequest adminAuth = new AuthRequest("bibliotecario", "admin123");
        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminAuth)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode adminJson = objectMapper.readTree(adminResult.getResponse().getContentAsString());
        bibliotecarioToken = adminJson.get("accessToken").asText();

        // Login socio
        AuthRequest socioAuth = new AuthRequest("socio_juan", "socio123");
        MvcResult socioResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(socioAuth)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode socioJson = objectMapper.readTree(socioResult.getResponse().getContentAsString());
        socioToken = socioJson.get("accessToken").asText();

        CredentialsEntity socioCred = credentialsRepository.findByUsername("socio_juan").orElseThrow();
        socio = socioCred.getUsuario();
    }

    private Book createBook(String title, int stock) {
        Category category = categoryRepository.findByNombreIgnoreCase("Tecnología")
                .orElseGet(() -> categoryRepository.save(Category.builder().nombre("Tecnología").descripcion("Tech").build()));

        String uniqueIsbn = "978-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return bookRepository.save(Book.builder()
                .titulo(title)
                .isbn(uniqueIsbn)
                .stockTotal(stock)
                .stockDisponible(stock)
                .categoria(category)
                .build());
    }

    @Test
    @DisplayName("Escenario 1: Devolución a tiempo (Estado DEVUELTO)")
    void testDevolucionATiempo() throws Exception {
        Book book = createBook("Java Performance", 2);

        Loan loan = Loan.builder()
                .libro(book)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(5))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(9))
                .estado(LoanStatus.PRESTADO)
                .build();
        loan = loanRepository.save(loan);

        mockMvc.perform(put("/api/prestamos/" + loan.getPublicId() + "/devolucion")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(loan.getPublicId().toString()))
                .andExpect(jsonPath("$.estado").value("DEVUELTO"))
                .andExpect(jsonPath("$.fechaDevolucionEfectiva").value(LocalDate.now().toString()));

        Book reloadedBook = bookRepository.findByPublicId(book.getPublicId()).orElseThrow();
        assertThat(reloadedBook.getStockDisponible()).isEqualTo(3);

        Loan reloadedLoan = loanRepository.findByPublicId(loan.getPublicId()).orElseThrow();
        assertThat(reloadedLoan.getEstado()).isEqualTo(LoanStatus.DEVUELTO);
        assertThat(reloadedLoan.getFechaDevolucionEfectiva()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Escenario 2: Devolución fuera de término (Estado CON_RETRASO)")
    void testDevolucionFueraDeTermino() throws Exception {
        Book book = createBook("Modern Operating Systems", 1);

        Loan loan = Loan.builder()
                .libro(book)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(17))
                .fechaDevolucionEsperada(LocalDate.now().minusDays(3))
                .estado(LoanStatus.PRESTADO)
                .build();
        loan = loanRepository.save(loan);

        mockMvc.perform(put("/api/prestamos/" + loan.getPublicId() + "/devolucion")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(loan.getPublicId().toString()))
                .andExpect(jsonPath("$.estado").value("CON_RETRASO"))
                .andExpect(jsonPath("$.fechaDevolucionEfectiva").value(LocalDate.now().toString()));

        Book reloadedBook = bookRepository.findByPublicId(book.getPublicId()).orElseThrow();
        assertThat(reloadedBook.getStockDisponible()).isEqualTo(2);

        Loan reloadedLoan = loanRepository.findByPublicId(loan.getPublicId()).orElseThrow();
        assertThat(reloadedLoan.getEstado()).isEqualTo(LoanStatus.CON_RETRASO);
    }

    @Test
    @DisplayName("Escenario 3: Intento de devolver un préstamo ya procesado")
    void testIntentoDevolverPrestamoYaDevuelto() throws Exception {
        Book book = createBook("Structure and Interpretation", 5);

        Loan loan = Loan.builder()
                .libro(book)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(10))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(4))
                .fechaDevolucionEfectiva(LocalDate.now().minusDays(1))
                .estado(LoanStatus.DEVUELTO)
                .build();
        loan = loanRepository.save(loan);

        mockMvc.perform(put("/api/prestamos/" + loan.getPublicId() + "/devolucion")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("El préstamo indicado ya ha sido devuelto anteriormente"));
    }

    @Test
    @DisplayName("Escenario 4: Socio no puede procesar devoluciones (403 Forbidden)")
    void testSocioNoPuedeDevolver() throws Exception {
        Book book = createBook("Computer Networks", 5);

        Loan loan = Loan.builder()
                .libro(book)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(5))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(9))
                .estado(LoanStatus.PRESTADO)
                .build();
        loan = loanRepository.save(loan);

        mockMvc.perform(put("/api/prestamos/" + loan.getPublicId() + "/devolucion")
                        .header("Authorization", "Bearer " + socioToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}

