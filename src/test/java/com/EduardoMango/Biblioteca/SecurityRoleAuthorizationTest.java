package com.EduardoMango.Biblioteca;

import com.EduardoMango.Biblioteca.dto.AuthRequest;
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
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityRoleAuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private String socioToken;
    private String bibliotecarioToken;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Obtener token para socio
        AuthRequest socioRequest = new AuthRequest("socio_juan", "socio123");
        MvcResult socioResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(socioRequest)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode socioJson = objectMapper.readTree(socioResult.getResponse().getContentAsString());
        socioToken = socioJson.get("accessToken").asText();

        // Obtener token para bibliotecario
        AuthRequest biblioRequest = new AuthRequest("bibliotecario", "admin123");
        MvcResult biblioResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(biblioRequest)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode biblioJson = objectMapper.readTree(biblioResult.getResponse().getContentAsString());
        bibliotecarioToken = biblioJson.get("accessToken").asText();
    }

    @Test
    @DisplayName("Endpoint público debe ser accesible sin autenticación")
    void testPublicEndpointAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/libros/publico/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libros").isArray());
    }

    @Test
    @DisplayName("Endpoint protegido debe retornar 401 Unauthorized sin token")
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/socio/mis-prestamos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("Socio debe poder consultar sus préstamos con ROLE_SOCIO")
    void testSocioCanAccessHisLoans() throws Exception {
        mockMvc.perform(get("/api/socio/mis-prestamos")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("socio_juan"))
                .andExpect(jsonPath("$.prestamos").isArray());
    }

    @Test
    @DisplayName("Socio debe poder solicitar un préstamo con ROLE_SOCIO")
    void testSocioCanRequestLoan() throws Exception {
        mockMvc.perform(post("/api/prestamos/solicitar?libroId=1")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_APROBACION"));
    }

    @Test
    @DisplayName("Socio debe recibir 403 Forbidden al intentar aprobar préstamos (exclusivo de Bibliotecario)")
    void testSocioCannotApproveLoans() throws Exception {
        mockMvc.perform(post("/api/prestamos/gestion/aprobar?prestamoId=101")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado: No posee los permisos o roles requeridos para este recurso"));
    }

    @Test
    @DisplayName("Socio debe recibir 403 Forbidden al intentar acceder al panel de administración")
    void testSocioCannotAccessAdminPanel() throws Exception {
        mockMvc.perform(get("/api/biblioteca/admin/panel")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bibliotecario debe poder aprobar préstamos con ROLE_BIBLIOTECARIO")
    void testBibliotecarioCanApproveLoans() throws Exception {
        mockMvc.perform(post("/api/prestamos/gestion/aprobar?prestamoId=101")
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.bibliotecario").value("bibliotecario"));
    }

    @Test
    @DisplayName("Bibliotecario debe poder acceder al panel de administración")
    void testBibliotecarioCanAccessAdminPanel() throws Exception {
        mockMvc.perform(get("/api/biblioteca/admin/panel")
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.administrador").value("bibliotecario"))
                .andExpect(jsonPath("$.totalLibros").value(1540));
    }
}
