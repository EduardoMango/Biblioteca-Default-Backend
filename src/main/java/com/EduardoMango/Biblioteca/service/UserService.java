package com.EduardoMango.Biblioteca.service;

import com.EduardoMango.Biblioteca.dto.RegisterRequest;
import com.EduardoMango.Biblioteca.dto.UserDTO;
import com.EduardoMango.Biblioteca.model.entity.CredentialsEntity;
import com.EduardoMango.Biblioteca.model.entity.RoleEntity;
import com.EduardoMango.Biblioteca.model.entity.UserEntity;
import com.EduardoMango.Biblioteca.model.enums.Roles;
import com.EduardoMango.Biblioteca.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.repository.RoleRepository;
import com.EduardoMango.Biblioteca.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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
                .build();

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(role);

        CredentialsEntity credentialsEntity = CredentialsEntity.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .enabled(true)
                .usuario(userEntity)
                .roles(roles)
                .build();

        CredentialsEntity savedCredentials = credentialsRepository.save(credentialsEntity);

        Set<String> roleNames = savedCredentials.getRoles().stream()
                .map(r -> r.getRole().name())
                .collect(Collectors.toSet());

        return new UserDTO(
                savedCredentials.getId(),
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

