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
import com.EduardoMango.Biblioteca.feature.user.dto.UserRoleUpdateRequest;
import com.EduardoMango.Biblioteca.feature.user.dto.UserStatusUpdateRequest;
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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class UserAdminControllerTest {

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

        // Obtener token bibliotecario inicial
        AuthRequest adminAuth = new AuthRequest("bibliotecario", "admin123");
        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminAuth)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode adminJson = objectMapper.readTree(adminResult.getResponse().getContentAsString());
        bibliotecarioToken = adminJson.get("accessToken").asText();

        // Obtener token socio inicial
        AuthRequest socioAuth = new AuthRequest("socio_juan", "socio123");
        MvcResult socioResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(socioAuth)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode socioJson = objectMapper.readTree(socioResult.getResponse().getContentAsString());
        socioToken = socioJson.get("accessToken").asText();
    }

    private UserEntity createTestUser(String username, String email, String rol) {
        Roles roleEnum = "BIBLIOTECARIO".equals(rol) ? Roles.ROLE_BIBLIOTECARIO : Roles.ROLE_SOCIO;
        RoleEntity roleEntity = roleRepository.findByRole(roleEnum)
                .orElseGet(() -> roleRepository.save(new RoleEntity(roleEnum)));

        UserEntity user = UserEntity.builder()
                .nombre("Usuario")
                .apellido("Test")
                .email(email)
                .dni(UUID.randomUUID().toString().substring(0, 8))
                .telefono("12345678")
                .rol(rol)
                .activo(true)
                .build();
        user = userRepository.save(user);

        CredentialsEntity credentials = CredentialsEntity.builder()
                .username(username)
                .password(passwordEncoder.encode("test1234"))
                .enabled(true)
                .usuario(user)
                .roles(new HashSet<>(Set.of(roleEntity)))
                .build();
        credentialsRepository.save(credentials);

        return user;
    }

    @Test
    @DisplayName("Escenario 1: Desactivar cuenta de un socio sin préstamos pendientes")
    void testDesactivarCuentaSocioSinPrestamos() throws Exception {
        UserEntity socio = createTestUser("socio_sin_prestamos", "socio7777@correo.com", "SOCIO");
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(false);

        mockMvc.perform(patch("/api/usuarios/" + socio.getPublicId() + "/estado")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(socio.getPublicId().toString()))
                .andExpect(jsonPath("$.activo").value(false));

        UserEntity reloaded = userRepository.findByPublicId(socio.getPublicId()).orElseThrow();
        assertThat(reloaded.getActivo()).isFalse();
    }

    @Test
    @DisplayName("Escenario 2: Intento de desactivar socio con préstamos activos")
    void testIntentoDesactivarSocioConPrestamosActivos() throws Exception {
        UserEntity socio = createTestUser("socio_con_prestamos", "socio8888@correo.com", "SOCIO");

        Category category = categoryRepository.findByNombreIgnoreCase("General")
                .orElseGet(() -> categoryRepository.save(Category.builder().nombre("General").descripcion("General").build()));
        Book book = bookRepository.save(Book.builder()
                .titulo("Java Concurrency in Practice")
                .isbn("978-0321349606")
                .stockTotal(5)
                .stockDisponible(5)
                .categoria(category)
                .build());

        Loan activeLoan = Loan.builder()
                .libro(book)
                .usuario(socio)
                .fechaPrestamo(LocalDate.now().minusDays(2))
                .fechaDevolucionEsperada(LocalDate.now().plusDays(12))
                .estado(LoanStatus.PRESTADO)
                .build();
        loanRepository.save(activeLoan);

        UserStatusUpdateRequest request = new UserStatusUpdateRequest(false);

        mockMvc.perform(patch("/api/usuarios/" + socio.getPublicId() + "/estado")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("No se puede desactivar un usuario con préstamos pendientes de devolución"));

        UserEntity reloaded = userRepository.findByPublicId(socio.getPublicId()).orElseThrow();
        assertThat(reloaded.getActivo()).isTrue();
    }

    @Test
    @DisplayName("Escenario 3: Cambiar el rol de un usuario de SOCIO a BIBLIOTECARIO")
    void testCambiarRolUsuario() throws Exception {
        UserEntity socio = createTestUser("socio_a_biblio", "socio9999@correo.com", "SOCIO");
        UserRoleUpdateRequest request = new UserRoleUpdateRequest("BIBLIOTECARIO");

        mockMvc.perform(patch("/api/usuarios/" + socio.getPublicId() + "/rol")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(socio.getPublicId().toString()))
                .andExpect(jsonPath("$.rol").value("BIBLIOTECARIO"));

        UserEntity reloaded = userRepository.findByPublicId(socio.getPublicId()).orElseThrow();
        assertThat(reloaded.getRol()).isEqualTo("BIBLIOTECARIO");
    }

    @Test
    @DisplayName("Escenario 4: Intento de autobloqueo de cuenta por parte del Bibliotecario")
    void testIntentoAutobloqueoBibliotecario() throws Exception {
        CredentialsEntity adminCredentials = credentialsRepository.findByUsername("bibliotecario").orElseThrow();
        UUID adminPublicId = adminCredentials.getUsuario().getPublicId();

        UserStatusUpdateRequest request = new UserStatusUpdateRequest(false);

        mockMvc.perform(patch("/api/usuarios/" + adminPublicId + "/estado")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Operación no permitida: Un usuario no puede desactivar su propia cuenta"));
    }

    @Test
    @DisplayName("Escenario 5: Socio no autorizado no puede modificar usuarios (403 Forbidden)")
    void testSocioNoPuedeModificarUsuarios() throws Exception {
        UserEntity target = createTestUser("usuario_target", "target@correo.com", "SOCIO");
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(false);

        mockMvc.perform(patch("/api/usuarios/" + target.getPublicId() + "/estado")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

