package com.EduardoMango.Biblioteca.feature.auth;

import com.EduardoMango.Biblioteca.feature.auth.domain.CredentialsEntity;
import com.EduardoMango.Biblioteca.feature.auth.domain.PasswordResetToken;
import com.EduardoMango.Biblioteca.feature.auth.domain.RoleEntity;
import com.EduardoMango.Biblioteca.feature.auth.domain.Roles;
import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.ForgotPasswordRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.ResetPasswordRequest;
import com.EduardoMango.Biblioteca.feature.auth.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.feature.auth.repository.PasswordResetTokenRepository;
import com.EduardoMango.Biblioteca.feature.auth.repository.RoleRepository;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import com.EduardoMango.Biblioteca.feature.user.UserRepository;
import com.EduardoMango.Biblioteca.infrastructure.email.model.EmailMessage;
import com.EduardoMango.Biblioteca.infrastructure.email.port.out.EmailSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class PasswordResetIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailSender emailSender;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private UserEntity createTestUser(String username, String email, String password) {
        RoleEntity role = roleRepository.findByRole(Roles.ROLE_SOCIO)
                .orElseGet(() -> roleRepository.save(new RoleEntity(Roles.ROLE_SOCIO)));

        UserEntity user = userRepository.save(UserEntity.builder()
                .nombre("Usuario")
                .apellido("Test")
                .email(email)
                .dni(UUID.randomUUID().toString().substring(0, 8))
                .telefono("12345678")
                .rol("SOCIO")
                .activo(true)
                .build());

        CredentialsEntity credentials = CredentialsEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .usuario(user)
                .roles(new HashSet<>(Set.of(role)))
                .build();
        credentialsRepository.save(credentials);

        return user;
    }

    @Test
    @DisplayName("Escenario 1: Solicitud exitosa de restablecimiento de contraseña")
    void testForgotPassword_Success() throws Exception {
        UserEntity user = createTestUser("reset_user", "usuario@correo.com", "OldPassword123!");

        ForgotPasswordRequest request = new ForgotPasswordRequest("usuario@correo.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, times(1)).send(captor.capture());

        EmailMessage message = captor.getValue();
        assertThat(message.to()).isEqualTo("usuario@correo.com");
        assertThat(message.subject()).contains("Contraseña");

        var tokens = tokenRepository.findByUserAndUsedFalse(user);
        assertThat(tokens).hasSize(1);
        PasswordResetToken token = tokens.get(0);
        assertThat(token.getToken()).isNotEmpty();
        assertThat(token.getExpiryDate()).isAfter(LocalDateTime.now().plusMinutes(14));
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    @DisplayName("RN 2: Anti-enumeración - Si el email no existe, responde 200 OK y no envía email")
    void testForgotPassword_NonExistentEmail_ReturnsOkWithoutEmail() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("noexiste@correo.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(emailSender, never()).send(any());
    }

    @Test
    @DisplayName("Escenario 2: Restablecimiento exitoso con token válido y login con nueva contraseña")
    void testResetPassword_Success_AndLoginWithNewPassword() throws Exception {
        UserEntity user = createTestUser("test_reset_login", "userlogin@test.com", "PasswordAntigua123!");

        String tokenValue = "token-123-xyz";
        PasswordResetToken token = PasswordResetToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();
        tokenRepository.save(token);

        ResetPasswordRequest request = new ResetPasswordRequest(tokenValue, "Password123!");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Verificar que el token se marcó como usado
        PasswordResetToken updatedToken = tokenRepository.findByToken(tokenValue).orElseThrow();
        assertThat(updatedToken.isUsed()).isTrue();

        // Verificar que el usuario puede hacer login con la nueva contraseña
        AuthRequest loginRequest = new AuthRequest("test_reset_login", "Password123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Escenario 3: Rechazo por token expirado")
    void testResetPassword_ExpiredToken_ReturnsBadRequest() throws Exception {
        UserEntity user = createTestUser("user_expired", "expired@test.com", "PasswordOriginal123!");

        String tokenValue = "token-expired-123";
        PasswordResetToken token = PasswordResetToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().minusMinutes(1)) // expirado
                .used(false)
                .build();
        tokenRepository.save(token);

        ResetPasswordRequest request = new ResetPasswordRequest(tokenValue, "NuevaPassword123!");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verificar que la clave original no cambió
        AuthRequest oldLoginRequest = new AuthRequest("user_expired", "PasswordOriginal123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLoginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Escenario 3b: Rechazo por token ya utilizado")
    void testResetPassword_AlreadyUsedToken_ReturnsBadRequest() throws Exception {
        UserEntity user = createTestUser("user_used", "used@test.com", "PasswordOriginal123!");

        String tokenValue = "token-used-123";
        PasswordResetToken token = PasswordResetToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(true) // ya utilizado
                .build();
        tokenRepository.save(token);

        ResetPasswordRequest request = new ResetPasswordRequest(tokenValue, "NuevaPassword123!");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

