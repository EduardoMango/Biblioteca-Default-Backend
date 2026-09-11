package com.EduardoMango.Biblioteca.feature.auth;

import com.EduardoMango.Biblioteca.feature.auth.domain.AccountVerificationToken;
import com.EduardoMango.Biblioteca.feature.auth.domain.CredentialsEntity;
import com.EduardoMango.Biblioteca.feature.auth.domain.RoleEntity;
import com.EduardoMango.Biblioteca.feature.auth.domain.Roles;
import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.ConfirmAccountRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.RegisterRequest;
import com.EduardoMango.Biblioteca.feature.auth.dto.ResendVerificationRequest;
import com.EduardoMango.Biblioteca.feature.auth.repository.AccountVerificationTokenRepository;
import com.EduardoMango.Biblioteca.feature.auth.repository.CredentialsRepository;
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
class AccountVerificationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AccountVerificationTokenRepository verificationTokenRepository;

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

    private UserEntity createInactiveUser(String username, String email, String password) {
        RoleEntity role = roleRepository.findByRole(Roles.ROLE_SOCIO)
                .orElseGet(() -> roleRepository.save(new RoleEntity(Roles.ROLE_SOCIO)));

        UserEntity user = userRepository.save(UserEntity.builder()
                .nombre("Inactivo")
                .apellido("Test")
                .email(email)
                .dni(UUID.randomUUID().toString().substring(0, 8))
                .telefono("12345678")
                .rol("SOCIO")
                .activo(false)
                .enabled(false)
                .build());

        CredentialsEntity credentials = CredentialsEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .enabled(false)
                .usuario(user)
                .roles(new HashSet<>(Set.of(role)))
                .build();
        credentialsRepository.save(credentials);

        return user;
    }

    @Test
    @DisplayName("Escenario 1: Registro exitoso guarda usuario con enabled = false")
    void testRegisterUser_InitialStateDisabled() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "nuevo_socio_bdd",
                "ClaveSegura123!",
                "Carlos",
                "Perez",
                "carlos.bdd@correo.com",
                "87654321",
                "1155667788",
                Roles.ROLE_SOCIO
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nuevo_socio_bdd"));

        CredentialsEntity creds = credentialsRepository.findByUsername("nuevo_socio_bdd").orElseThrow();
        assertThat(creds.isEnabled()).isFalse();
        assertThat(creds.getUsuario().getActivo()).isFalse();
    }

    @Test
    @DisplayName("Escenario 2: Intento de inicio de sesión antes de confirmar la cuenta rechaza con 401 Unauthorized")
    void testLoginBeforeAccountConfirmation_ReturnsUnauthorized() throws Exception {
        createInactiveUser("usuario_inactivo", "inactivo@test.com", "ClaveValida123!");

        AuthRequest loginRequest = new AuthRequest("usuario_inactivo", "ClaveValida123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("Escenario 3: Confirmación exitosa de la cuenta dentro de los 15 minutos")
    void testConfirmAccount_Success() throws Exception {
        UserEntity user = createInactiveUser("user_confirm_ok", "confirm_ok@test.com", "Clave123!");

        String tokenValue = "confirm-abc-123";
        AccountVerificationToken token = AccountVerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(10)) // generado hace 5 min
                .used(false)
                .build();
        verificationTokenRepository.save(token);

        ConfirmAccountRequest request = new ConfirmAccountRequest(tokenValue);

        mockMvc.perform(post("/api/v1/auth/confirm-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Verificar estado activo
        CredentialsEntity creds = credentialsRepository.findByUsername("user_confirm_ok").orElseThrow();
        assertThat(creds.isEnabled()).isTrue();
        assertThat(creds.getUsuario().getActivo()).isTrue();

        // Verificar token usado
        AccountVerificationToken updatedToken = verificationTokenRepository.findByToken(tokenValue).orElseThrow();
        assertThat(updatedToken.isUsed()).isTrue();

        // Verificar que ahora sí puede iniciar sesión
        AuthRequest loginRequest = new AuthRequest("user_confirm_ok", "Clave123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Escenario 4: Intento de confirmación con token expirado")
    void testConfirmAccount_ExpiredToken_ReturnsBadRequest() throws Exception {
        UserEntity user = createInactiveUser("user_expired_token", "expired_token@test.com", "Clave123!");

        String tokenValue = "confirm-expired-999";
        AccountVerificationToken token = AccountVerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().minusMinutes(2)) // expirado
                .used(false)
                .build();
        verificationTokenRepository.save(token);

        mockMvc.perform(post("/api/v1/auth/confirm-account?token=" + tokenValue))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        CredentialsEntity creds = credentialsRepository.findByUsername("user_expired_token").orElseThrow();
        assertThat(creds.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("RN 4: Re-envío de código de verificación invalida anteriores y envía nuevo email")
    void testResendVerification_Success() throws Exception {
        UserEntity user = createInactiveUser("user_resend", "resend@test.com", "Clave123!");

        String oldToken = "old-token-111";
        verificationTokenRepository.save(AccountVerificationToken.builder()
                .token(oldToken)
                .user(user)
                .expiryDate(LocalDateTime.now().minusMinutes(10))
                .used(false)
                .build());

        ResendVerificationRequest request = new ResendVerificationRequest("resend@test.com");

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, times(1)).send(captor.capture());

        EmailMessage sent = captor.getValue();
        assertThat(sent.to()).isEqualTo("resend@test.com");
        assertThat(sent.subject()).contains("Verificación");

        // Anterior token marcado como usado/invalidado
        AccountVerificationToken old = verificationTokenRepository.findByToken(oldToken).orElseThrow();
        assertThat(old.isUsed()).isTrue();

        // Nuevo token activo
        var activeTokens = verificationTokenRepository.findByUserAndUsedFalse(user);
        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.get(0).getToken()).isNotEqualTo(oldToken);
    }
}

