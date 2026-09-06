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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class LoanSupervisionControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

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

    private Book createBook(String title) {
        Category category = categoryRepository.findByNombreIgnoreCase("Tecnología")
                .orElseGet(() -> categoryRepository.save(Category.builder().nombre("Tecnología").descripcion("Tech").build()));

        String uniqueIsbn = "978-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return bookRepository.save(Book.builder()
                .titulo(title)
                .isbn(uniqueIsbn)
                .stockTotal(10)
                .stockDisponible(10)
                .categoria(category)
                .build());
    }

    @Test
    @DisplayName("Escenario 1: Consultar únicamente préstamos atrasados")
    void testConsultarUnicamentePrestamosAtrasados() throws Exception {
        UserEntity socio = createSocio("socio_supervision_1", "super1@test.com");
        Book book = createBook("Book For Supervision");

        // 2 préstamos activos vigentes
        Loan loanVigente1 = Loan.builder()
                .libro(book)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(2))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(12))
                .estado(LoanStatus.PRESTADO)
                .build();
        Loan loanVigente2 = Loan.builder()
                .libro(book)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(5))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(9))
                .estado(LoanStatus.PRESTADO)
                .build();

        // 2 préstamos activos cuya fechaDevolucionEsperada ya venció
        Loan loanAtrasado1 = Loan.builder()
                .libro(book)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(20))
                .fechaDevolucionEsperada(LocalDate.now().minusDays(6))
                .estado(LoanStatus.PRESTADO)
                .build();
        Loan loanAtrasado2 = Loan.builder()
                .libro(book)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(18))
                .fechaDevolucionEsperada(LocalDate.now().minusDays(4))
                .estado(LoanStatus.PRESTADO)
                .build();

        loanRepository.saveAll(List.of(loanVigente1, loanVigente2, loanAtrasado1, loanAtrasado2));

        mockMvc.perform(get("/api/prestamos/supervision")
                        .param("soloAtrasados", "true")
                        .param("usuarioPublicId", socio.getPublicId().toString())
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].diasAtraso").isNumber());
    }

    @Test
    @DisplayName("Escenario 2: Filtrar préstamos activos por un socio específico")
    void testFiltrarPrestamosActivosPorSocio() throws Exception {
        UserEntity socioA = createSocio("socio_especifico_a", "socioA@test.com");
        UserEntity socioB = createSocio("socio_especifico_b", "socioB@test.com");
        Book book = createBook("Shared Book");

        Loan loanA = Loan.builder()
                .libro(book)
                .usuario(socioA)
                .fechaPrestamo(LocalDate.now().minusDays(1))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(13))
                .estado(LoanStatus.PRESTADO)
                .build();

        Loan loanB = Loan.builder()
                .libro(book)
                .usuario(socioB)
                .fechaPrestamo(LocalDate.now().minusDays(1))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(13))
                .estado(LoanStatus.PRESTADO)
                .build();

        loanRepository.saveAll(List.of(loanA, loanB));

        mockMvc.perform(get("/api/prestamos/supervision")
                        .param("usuarioPublicId", socioA.getPublicId().toString())
                        .param("estado", "PRESTADO")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].publicId").value(loanA.getPublicId().toString()))
                .andExpect(jsonPath("$.content[0].usuario.publicId").value(socioA.getPublicId().toString()));
    }

    @Test
    @DisplayName("Escenario 3: Acceso denegado a usuarios con rol SOCIO")
    void testAccesoDenegadoSocio() throws Exception {
        mockMvc.perform(get("/api/prestamos/supervision")
                        .header("Authorization", "Bearer " + socioToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }
}

