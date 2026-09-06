package com.EduardoMango.Biblioteca.feature.user.service;

import com.EduardoMango.Biblioteca.exception.BusinessRuleException;
import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.feature.user.dto.UserAdminResponse;
import com.EduardoMango.Biblioteca.feature.user.mapper.UserAdminMapper;
import com.EduardoMango.Biblioteca.model.entity.CredentialsEntity;
import com.EduardoMango.Biblioteca.model.entity.RoleEntity;
import com.EduardoMango.Biblioteca.model.entity.UserEntity;
import com.EduardoMango.Biblioteca.model.enums.Roles;
import com.EduardoMango.Biblioteca.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.repository.RoleRepository;
import com.EduardoMango.Biblioteca.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CredentialsRepository credentialsRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserAdminMapper userAdminMapper;

    @InjectMocks
    private UserAdminService userAdminService;

    @Test
    @DisplayName("Escenario 1: Desactivar cuenta de un socio sin préstamos pendientes")
    void updateUserStatus_WhenValidAndNoPendingLoans_Success() {
        UUID targetPublicId = UUID.randomUUID();
        UserEntity targetUser = UserEntity.builder()
                .publicId(targetPublicId)
                .nombre("Pedro")
                .activo(true)
                .build();

        UUID adminPublicId = UUID.randomUUID();
        UserEntity adminUser = UserEntity.builder()
                .publicId(adminPublicId)
                .nombre("Admin")
                .build();
        CredentialsEntity adminCredentials = CredentialsEntity.builder().usuario(adminUser).build();

        CredentialsEntity targetCredentials = CredentialsEntity.builder().usuario(targetUser).enabled(true).build();

        given(userRepository.findByPublicId(targetPublicId)).willReturn(Optional.of(targetUser));
        given(credentialsRepository.findByUsername("admin")).willReturn(Optional.of(adminCredentials));
        given(loanRepository.existsByUsuarioAndEstadoIn(targetUser, List.of(LoanStatus.PRESTADO, LoanStatus.CON_RETRASO))).willReturn(false);
        given(credentialsRepository.findByUsuario(targetUser)).willReturn(Optional.of(targetCredentials));
        given(userAdminMapper.toUserAdminResponse(targetUser)).willReturn(new UserAdminResponse(targetPublicId, "Pedro", "López", "pedro@test.com", "SOCIO", false));

        UserAdminResponse response = userAdminService.updateUserStatus(targetPublicId, false, "admin");

        assertThat(response).isNotNull();
        assertThat(response.activo()).isFalse();
        assertThat(targetUser.getActivo()).isFalse();
        assertThat(targetCredentials.getEnabled()).isFalse();
        verify(userRepository).save(targetUser);
    }

    @Test
    @DisplayName("Escenario 2: Intento de desactivar socio con préstamos activos")
    void updateUserStatus_WhenHasPendingLoans_ThrowsBusinessRuleException() {
        UUID targetPublicId = UUID.randomUUID();
        UserEntity targetUser = UserEntity.builder()
                .publicId(targetPublicId)
                .activo(true)
                .build();

        UserEntity adminUser = UserEntity.builder()
                .publicId(UUID.randomUUID())
                .build();
        CredentialsEntity adminCredentials = CredentialsEntity.builder().usuario(adminUser).build();

        given(userRepository.findByPublicId(targetPublicId)).willReturn(Optional.of(targetUser));
        given(credentialsRepository.findByUsername("admin")).willReturn(Optional.of(adminCredentials));
        given(loanRepository.existsByUsuarioAndEstadoIn(targetUser, List.of(LoanStatus.PRESTADO, LoanStatus.CON_RETRASO))).willReturn(true);

        assertThatThrownBy(() -> userAdminService.updateUserStatus(targetPublicId, false, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No se puede desactivar un usuario con préstamos pendientes de devolución");

        assertThat(targetUser.getActivo()).isTrue();
    }

    @Test
    @DisplayName("Escenario 3: Cambiar el rol de un usuario de SOCIO a BIBLIOTECARIO")
    void updateUserRole_WhenValid_Success() {
        UUID targetPublicId = UUID.randomUUID();
        UserEntity targetUser = UserEntity.builder()
                .publicId(targetPublicId)
                .rol("SOCIO")
                .build();

        UserEntity adminUser = UserEntity.builder()
                .publicId(UUID.randomUUID())
                .build();
        CredentialsEntity adminCredentials = CredentialsEntity.builder().usuario(adminUser).build();

        RoleEntity biblioRole = new RoleEntity(Roles.ROLE_BIBLIOTECARIO);
        CredentialsEntity targetCredentials = CredentialsEntity.builder().usuario(targetUser).build();

        given(userRepository.findByPublicId(targetPublicId)).willReturn(Optional.of(targetUser));
        given(credentialsRepository.findByUsername("admin")).willReturn(Optional.of(adminCredentials));
        given(roleRepository.findByRole(Roles.ROLE_BIBLIOTECARIO)).willReturn(Optional.of(biblioRole));
        given(credentialsRepository.findByUsuario(targetUser)).willReturn(Optional.of(targetCredentials));
        given(userAdminMapper.toUserAdminResponse(targetUser)).willReturn(new UserAdminResponse(targetPublicId, "Ana", "M", "ana@test.com", "BIBLIOTECARIO", true));

        UserAdminResponse response = userAdminService.updateUserRole(targetPublicId, "BIBLIOTECARIO", "admin");

        assertThat(response).isNotNull();
        assertThat(response.rol()).isEqualTo("BIBLIOTECARIO");
        assertThat(targetUser.getRol()).isEqualTo("BIBLIOTECARIO");
        assertThat(targetCredentials.getRoles()).contains(biblioRole);
        verify(userRepository).save(targetUser);
    }

    @Test
    @DisplayName("Escenario 4: Intento de autobloqueo de cuenta por parte del Bibliotecario")
    void updateUserStatus_WhenAdminTriesToDeactivateThemselves_ThrowsBusinessRuleException() {
        UUID adminPublicId = UUID.randomUUID();
        UserEntity adminUser = UserEntity.builder()
                .publicId(adminPublicId)
                .activo(true)
                .build();
        CredentialsEntity adminCredentials = CredentialsEntity.builder().usuario(adminUser).build();

        given(userRepository.findByPublicId(adminPublicId)).willReturn(Optional.of(adminUser));
        given(credentialsRepository.findByUsername("admin")).willReturn(Optional.of(adminCredentials));

        assertThatThrownBy(() -> userAdminService.updateUserStatus(adminPublicId, false, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Operación no permitida: Un usuario no puede desactivar su propia cuenta");
    }

    @Test
    @DisplayName("Intento de automodificación de rol por parte del Bibliotecario")
    void updateUserRole_WhenAdminTriesToModifyOwnRole_ThrowsBusinessRuleException() {
        UUID adminPublicId = UUID.randomUUID();
        UserEntity adminUser = UserEntity.builder()
                .publicId(adminPublicId)
                .rol("BIBLIOTECARIO")
                .build();
        CredentialsEntity adminCredentials = CredentialsEntity.builder().usuario(adminUser).build();

        given(userRepository.findByPublicId(adminPublicId)).willReturn(Optional.of(adminUser));
        given(credentialsRepository.findByUsername("admin")).willReturn(Optional.of(adminCredentials));

        assertThatThrownBy(() -> userAdminService.updateUserRole(adminPublicId, "SOCIO", "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Operación no permitida: Un usuario no puede modificar su propio rol");
    }
}

