package com.EduardoMango.Biblioteca.feature.user.service;

import com.EduardoMango.Biblioteca.feature.auth.dto.RegisterRequest;
import com.EduardoMango.Biblioteca.feature.user.dto.UserDTO;
import com.EduardoMango.Biblioteca.feature.auth.domain.CredentialsEntity;
import com.EduardoMango.Biblioteca.feature.auth.domain.RoleEntity;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import com.EduardoMango.Biblioteca.feature.auth.domain.Roles;
import com.EduardoMango.Biblioteca.feature.auth.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.feature.auth.repository.RoleRepository;
import com.EduardoMango.Biblioteca.feature.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.EduardoMango.Biblioteca.feature.user.event.UserRegisteredEvent;
import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserDTO save(RegisterRequest request) {
        if (credentialsRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("El nombre de usuario '" + request.username() + "' ya está registrado");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El correo electrónico '" + request.email() + "' ya está registrado");
        }

        Roles roleEnum = request.role() != null ? request.role() : Roles.ROLE_SOCIO;
        RoleEntity role = roleRepository.findByRole(roleEnum)
                .orElseGet(() -> roleRepository.save(new RoleEntity(roleEnum)));

        UserEntity userEntity = UserEntity.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .email(request.email())
                .dni(request.dni())
                .telefono(request.telefono())
                .activo(false)
                .enabled(false)
                .build();

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(role);

        CredentialsEntity credentialsEntity = CredentialsEntity.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .enabled(true)
                .enabled(false)
                .usuario(userEntity)
                .roles(roles)
                .build();

        CredentialsEntity savedCredentials = credentialsRepository.save(credentialsEntity);

        eventPublisher.publishEvent(new UserRegisteredEvent(
                savedCredentials.getUsuario().getPublicId(),
                userEntity.getEmail(),
                userEntity.getNombre()
        ));

        Set<String> roleNames = savedCredentials.getRoles().stream()
                .map(r -> r.getRole().name())
                .collect(Collectors.toSet());

        return new UserDTO(
                savedCredentials.getUsuario().getPublicId(),
                savedCredentials.getUsername(),
                userEntity.getNombre(),
                userEntity.getApellido(),
                userEntity.getEmail(),
                userEntity.getDni(),
                userEntity.getTelefono(),
                roleNames
        );
    }
}

