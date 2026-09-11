package com.EduardoMango.Biblioteca.feature.user.controller;

import com.EduardoMango.Biblioteca.feature.auth.domain.CredentialsEntity;
import com.EduardoMango.Biblioteca.feature.auth.domain.RoleEntity;
import com.EduardoMango.Biblioteca.feature.auth.domain.Roles;
import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.auth.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.feature.auth.repository.RoleRepository;
import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.category.Category;
import com.EduardoMango.Biblioteca.feature.category.CategoryRepository;
import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import com.EduardoMango.Biblioteca.feature.user.UserRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class UserProfileControllerTest {

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

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        AuthRequest authRequest = new AuthRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("accessToken").asText();
    }

    private UserEntity createSocioUser(String username, String email, String rawPassword) {
        RoleEntity roleSocio = roleRepository.findByRole(Roles.ROLE_SOCIO)
                .orElseGet(() -> roleRepository.save(new RoleEntity(Roles.ROLE_SOCIO)));

        UserEntity user = UserEntity.builder()
                .nombre("Carlos")
                .apellido("Gómez")
                .email(email)
                .dni(UUID.randomUUID().toString().substring(0, 8))
                .telefono("123456789")
                .rol("SOCIO")
                .activo(true)
                .build();
        user = userRepository.save(user);

        CredentialsEntity credentials = CredentialsEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .enabled(true)
                .usuario(user)
                .roles(new HashSet<>(Set.of(roleSocio)))
                .build();
        credentialsRepository.save(credentials);

        return user;
    }

    private Book createBook(String title, String isbn) {
        Category category = categoryRepository.findByNombreIgnoreCase("Tecnología")
                .orElseGet(() -> categoryRepository.save(Category.builder().nombre("Tecnología").descripcion("Libros tech").build()));

        Book book = Book.builder()
                .titulo(title)
                .isbn(isbn)
                .stockTotal(10)
                .stockDisponible(10)
                .categoria(category)
                .build();
        return bookRepository.save(book);
    }

    @Test
    @DisplayName("Escenario 1: Consultar perfil propio con préstamos activos e históricos")
    void testConsultarPerfilPropioConPrestamos() throws Exception {
        // Given un socio autenticado con email "socio@correo.com"
        UserEntity socio = createSocioUser("socio_perfil_test", "socio@correo.com", "clave123");
        String token = loginAndGetToken("socio_perfil_test", "clave123");

        Book book1 = createBook("Domain-Driven Design", "978-0321125217");
        Book book2 = createBook("Patterns of Enterprise Application Architecture", "978-0321127426");

        // And posee 1 préstamo activo (PRESTADO) y 1 préstamo finalizado (DEVUELTO)
        Loan loanActivo = Loan.builder()
                .libro(book1)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(5))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(9))
                .estado(LoanStatus.PRESTADO)
                .build();

        Loan loanDevuelto = Loan.builder()
                .libro(book2)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(20))
                .fechaDevolucionEsperada(LocalDate.now().minusDays(6))
                .fechaDevolucionEfectiva(LocalDate.now().minusDays(7))
                .estado(LoanStatus.DEVUELTO)
                .build();

        loanRepository.saveAll(List.of(loanActivo, loanDevuelto));

        // When realiza una petición GET /api/usuarios/me
        mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                // Then el sistema reconoce al usuario a partir del token de sesión
                // And responde HTTP 200 OK con la información del usuario
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(socio.getPublicId().toString()))
                .andExpect(jsonPath("$.email").value("socio@correo.com"))
                .andExpect(jsonPath("$.nombre").value("Carlos"))
                .andExpect(jsonPath("$.apellido").value("Gómez"))
                .andExpect(jsonPath("$.rol").value("SOCIO"))
                // And el array de préstamos contiene exactamente 2 elementos con la información de los libros consumidos
                .andExpect(jsonPath("$.prestamos", hasSize(2)))
                .andExpect(jsonPath("$.prestamos[*].tituloLibro", containsInAnyOrder("Domain-Driven Design", "Patterns of Enterprise Application Architecture")));
    }

    @Test
    @DisplayName("Escenario 2: Intento de consulta sin autenticación")
    void testConsultaPerfilSinAutenticacion() throws Exception {
        // Given un cliente HTTP no autenticado (sin token JWT o credenciales válidas)
        // When realiza una petición GET /api/usuarios/me
        mockMvc.perform(get("/api/usuarios/me")
                        .accept(MediaType.APPLICATION_JSON))
                // Then el middleware de seguridad intercepta la petición
                // And responde HTTP 401 Unauthorized en formato RFC 7807 (ProblemDetail)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Escenario 3: Paginación opcional en el historial de préstamos")
    void testHistorialPrestamosPaginado() throws Exception {
        // Given un socio con más de 10 préstamos registrados en su historial
        UserEntity socio = createSocioUser("socio_paginado_test", "paginado@correo.com", "clave123");
        String token = loginAndGetToken("socio_paginado_test", "clave123");

        Book book = createBook("Libro de Prueba Paginación", "978-0000000001");

        for (int i = 1; i <= 12; i++) {
            Loan loan = Loan.builder()
                    .libro(book)
                    .usuario(socio)
                    .fechaPrestamo(LocalDate.now().minusDays(i))
                    .fechaDevolucionEsperada(LocalDate.now().plusDays(14 - i))
                    .estado(LoanStatus.PRESTADO)
                    .build();
            loanRepository.save(loan);
        }

        // When realiza una petición GET /api/usuarios/me/prestamos?page=0&size=5
        mockMvc.perform(get("/api/usuarios/me/prestamos")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                // Then el sistema responde HTTP 200 OK
                .andExpect(status().isOk())
                // And devuelve únicamente los 5 préstamos más recientes con la metadata de paginación
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.page.totalElements").value(12))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.number").value(0));
    }
}

