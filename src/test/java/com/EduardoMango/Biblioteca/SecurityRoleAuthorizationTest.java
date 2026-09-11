package com.EduardoMango.Biblioteca;

import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
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

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
        AuthRequest loginRequest = new AuthRequest("socio_juan", "socio123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Endpoint protegido debe retornar 401 Unauthorized sin token")
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("Socio debe poder consultar su perfil con ROLE_SOCIO")
    void testSocioCanAccessHisProfile() throws Exception {
        mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.rol").value("SOCIO"));
    }

    @Test
    @DisplayName("Socio debe poder consultar catálogo de libros con ROLE_SOCIO")
    void testSocioCanAccessBooksCatalog() throws Exception {
        mockMvc.perform(get("/api/libros")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Socio debe recibir 403 Forbidden al intentar acceder a la supervisión de préstamos (exclusivo de Bibliotecario)")
    void testSocioCannotAccessSupervision() throws Exception {
        mockMvc.perform(get("/api/prestamos/supervision")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado: No posee los permisos o roles requeridos para este recurso"));
    }

    @Test
    @DisplayName("Socio debe recibir 403 Forbidden al intentar modificar el estado de un usuario (exclusivo de Bibliotecario)")
    void testSocioCannotModifyUserStatus() throws Exception {
        mockMvc.perform(patch("/api/usuarios/" + UUID.randomUUID() + "/estado")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\": false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado: No posee los permisos o roles requeridos para este recurso"));
    }

    @Test
    @DisplayName("Bibliotecario debe poder acceder a la supervisión de préstamos con ROLE_BIBLIOTECARIO")
    void testBibliotecarioCanAccessSupervision() throws Exception {
        mockMvc.perform(get("/api/prestamos/supervision")
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Bibliotecario debe poder consultar su perfil con ROLE_BIBLIOTECARIO")
    void testBibliotecarioCanAccessProfile() throws Exception {
        mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Laura"))
                .andExpect(jsonPath("$.rol").value("BIBLIOTECARIO"));
    }
}
