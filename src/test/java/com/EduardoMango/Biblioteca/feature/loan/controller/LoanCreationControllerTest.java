package com.EduardoMango.Biblioteca.feature.loan.controller;

import com.EduardoMango.Biblioteca.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.book.domain.Book;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.category.domain.Category;
import com.EduardoMango.Biblioteca.feature.category.repository.CategoryRepository;
import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanCreateRequest;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.model.entity.CredentialsEntity;
import com.EduardoMango.Biblioteca.model.entity.RoleEntity;
import com.EduardoMango.Biblioteca.model.entity.UserEntity;
import com.EduardoMango.Biblioteca.model.enums.Roles;
import com.EduardoMango.Biblioteca.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.repository.RoleRepository;
import com.EduardoMango.Biblioteca.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class LoanCreationControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private String bibliotecarioToken;
    private String socioToken;

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
    }

    private UserEntity createSocio(String username, String email) {
        RoleEntity roleSocio = roleRepository.findByRole(Roles.ROLE_SOCIO)
                .orElseGet(() -> roleRepository.save(new RoleEntity(Roles.ROLE_SOCIO)));

        UserEntity user = UserEntity.builder()
                .nombre("Socio")
                .apellido("Test")
                .email(email)
                .dni(UUID.randomUUID().toString().substring(0, 8))
                .telefono("11223344")
                .rol("SOCIO")
                .activo(true)
                .build();
        user = userRepository.save(user);

        CredentialsEntity credentials = CredentialsEntity.builder()
                .username(username)
                .password(passwordEncoder.encode("clave123"))
                .enabled(true)
                .usuario(user)
                .roles(new HashSet<>(Set.of(roleSocio)))
                .build();
        credentialsRepository.save(credentials);

        return user;
    }

    private Book createBook(String title, String isbn, int stock) {
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
    @DisplayName("Escenario 1: Registro exitoso de préstamo por un Socio")
    void testRegistroExitosoPrestamoPorSocio() throws Exception {
        Book book = createBook("Domain-Driven Design", "978-0321125217", 3);
        LoanCreateRequest request = new LoanCreateRequest(book.getPublicId(), null);

        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId", notNullValue()))
                .andExpect(jsonPath("$.estado").value("PRESTADO"))
                .andExpect(jsonPath("$.libroPublicId").value(book.getPublicId().toString()))
                .andExpect(jsonPath("$.fechaDevolucionEsperada").value(LocalDate.now().plusDays(14).toString()));

        Book reloadedBook = bookRepository.findByPublicId(book.getPublicId()).orElseThrow();
        assertThat(reloadedBook.getStockDisponible()).isEqualTo(2);
    }

    @Test
    @DisplayName("Escenario 2: Bibliotecario registra préstamo en nombre de un socio")
    void testBibliotecarioRegistraPrestamoParaSocio() throws Exception {
        UserEntity targetSocio = createSocio("socio_dest", "destino@test.com");
        Book book = createBook("Continuous Delivery", "978-0321601919", 2);

        LoanCreateRequest request = new LoanCreateRequest(book.getPublicId(), targetSocio.getPublicId());

        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId", notNullValue()))
                .andExpect(jsonPath("$.usuarioPublicId").value(targetSocio.getPublicId().toString()));

        Book reloadedBook = bookRepository.findByPublicId(book.getPublicId()).orElseThrow();
        assertThat(reloadedBook.getStockDisponible()).isEqualTo(1);
    }

    @Test
    @DisplayName("Escenario 3: Rechazo por falta de stock disponible")
    void testRechazoPorFaltaDeStock() throws Exception {
        Book book = createBook("Agotado Book", "978-9999999999", 0);
        LoanCreateRequest request = new LoanCreateRequest(book.getPublicId(), null);

        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("No hay ejemplares disponibles del libro solicitado"));

        Book reloadedBook = bookRepository.findByPublicId(book.getPublicId()).orElseThrow();
        assertThat(reloadedBook.getStockDisponible()).isEqualTo(0);
    }

    @Test
    @DisplayName("Escenario 4: Rechazo por superar el límite máximo de préstamos activos (3)")
    void testRechazoPorLimiteMaximoPrestamos() throws Exception {
        CredentialsEntity socioCred = credentialsRepository.findByUsername("socio_juan").orElseThrow();
        UserEntity socio = socioCred.getUsuario();

        Book bookForActiveLoans = createBook("Libro Base", "978-1111111111", 10);
        for (int i = 0; i < 3; i++) {
            Loan loan = Loan.builder()
                    .libro(bookForActiveLoans)
                    .usuario(socio)
                    .fechaPrestamo(LocalDate.now().minusDays(i + 1))
                    .fechaDevolucionEsperada(LocalDate.now().plusDays(10))
                    .estado(LoanStatus.PRESTADO)
                    .build();
            loanRepository.save(loan);
        }

        Book bookNuevo = createBook("Cuarto Libro", "978-2222222222", 5);
        LoanCreateRequest request = new LoanCreateRequest(bookNuevo.getPublicId(), null);

        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("El usuario ha alcanzado el límite máximo de 3 préstamos activos"));
    }

    @Test
    @DisplayName("Escenario 5: Rechazo por poseer préstamos vencidos sin devolver")
    void testRechazoPorPrestamosVencidos() throws Exception {
        CredentialsEntity socioCred = credentialsRepository.findByUsername("socio_juan").orElseThrow();
        UserEntity socio = socioCred.getUsuario();

        Book bookVencido = createBook("Libro Atrasado", "978-3333333333", 5);
        Loan loanVencido = Loan.builder()
                .libro(bookVencido)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(20))
                .fechaDevolucionEsperada(LocalDate.now().minusDays(2))
                .fechaDevolucionEfectiva(null)
                .estado(LoanStatus.PRESTADO)
                .build();
        loanRepository.save(loanVencido);

        Book bookNuevo = createBook("Otro Libro", "978-4444444444", 5);
        LoanCreateRequest request = new LoanCreateRequest(bookNuevo.getPublicId(), null);

        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("El usuario posee préstamos atrasados pendientes de devolución"));
    }
}

