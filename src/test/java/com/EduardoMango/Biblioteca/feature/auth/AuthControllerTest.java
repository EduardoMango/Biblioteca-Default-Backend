package com.EduardoMango.Biblioteca.feature.auth;

import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.RefreshTokenRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.RegisterRequest;
import com.EduardoMango.Biblioteca.feature.auth.domain.Roles;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Debe autenticar exitosamente y retornar tokens JWT para usuario válido")
    void testLoginSuccess() throws Exception {
        AuthRequest request = new AuthRequest("bibliotecario", "admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Debe rechazar credenciales incorrectas con 401 Unauthorized")
    void testLoginInvalidPassword() throws Exception {
        AuthRequest request = new AuthRequest("bibliotecario", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("Debe registrar un nuevo usuario exitosamente")
    void testRegisterUserSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "maria_lopez",
                "clave123",
                "María",
                "López",
                "maria.lopez@biblioteca.com",
                "33445566",
                "1144556677",
                Roles.ROLE_SOCIO
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("maria_lopez"))
                .andExpect(jsonPath("$.nombre").value("María"))
                .andExpect(jsonPath("$.email").value("maria.lopez@biblioteca.com"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    @DisplayName("Debe permitir refrescar tokens usando el refresh token")
    void testRefreshTokenSuccess() throws Exception {
        // 1. Login inicial para obtener refresh token
        AuthRequest loginRequest = new AuthRequest("socio_juan", "socio123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = jsonNode.get("refreshToken").asText();
        assertNotNull(refreshToken);

        // 2. Refrescar token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Debe rechazar un refresh token inválido con 400 Bad Request")
    void testRefreshTokenInvalid() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("token.invalido.falso");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }
}
