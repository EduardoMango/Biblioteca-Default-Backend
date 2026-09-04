package com.EduardoMango.Biblioteca.config;

import com.EduardoMango.Biblioteca.model.entity.CredentialsEntity;
import com.EduardoMango.Biblioteca.model.entity.PermitEntity;
import com.EduardoMango.Biblioteca.model.entity.RoleEntity;
import com.EduardoMango.Biblioteca.model.entity.UserEntity;
import com.EduardoMango.Biblioteca.model.enums.Permits;
import com.EduardoMango.Biblioteca.model.enums.Roles;
import com.EduardoMango.Biblioteca.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.repository.PermitRepository;
import com.EduardoMango.Biblioteca.repository.RoleRepository;
import com.EduardoMango.Biblioteca.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PermitRepository permitRepository;
    private final RoleRepository roleRepository;
    private final CredentialsRepository credentialsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Inicializando permisos, roles y usuarios iniciales de la biblioteca...");

        // 1. Crear permisos si no existen
        Map<Permits, PermitEntity> permitMap = new EnumMap<>(Permits.class);
        for (Permits permitEnum : Permits.values()) {
            PermitEntity permit = permitRepository.findByPermit(permitEnum)
                    .orElseGet(() -> permitRepository.save(new PermitEntity(permitEnum)));
            permitMap.put(permitEnum, permit);
        }

        // 2. Crear o actualizar Role ROLE_SOCIO
        RoleEntity roleSocio = roleRepository.findByRole(Roles.ROLE_SOCIO)
                .orElseGet(() -> new RoleEntity(Roles.ROLE_SOCIO));
        roleSocio.addPermit(permitMap.get(Permits.VER_LIBROS));
        roleSocio.addPermit(permitMap.get(Permits.SOLICITAR_PRESTAMO));
        roleSocio.addPermit(permitMap.get(Permits.VER_PROPIOS_PRESTAMOS));
        roleRepository.save(roleSocio);

        // 3. Crear o actualizar Role ROLE_BIBLIOTECARIO
        RoleEntity roleBibliotecario = roleRepository.findByRole(Roles.ROLE_BIBLIOTECARIO)
                .orElseGet(() -> new RoleEntity(Roles.ROLE_BIBLIOTECARIO));
        for (PermitEntity permit : permitMap.values()) {
            roleBibliotecario.addPermit(permit);
        }
        roleRepository.save(roleBibliotecario);

        // 4. Crear usuario Bibliotecario por defecto si no existe
        if (!credentialsRepository.existsByUsername("bibliotecario")) {
            UserEntity adminUser = UserEntity.builder()
                    .nombre("Laura")
                    .apellido("Gómez")
                    .email("bibliotecario@biblioteca.com")
                    .dni("30123456")
                    .telefono("1122334455")
                    .build();

            CredentialsEntity adminCredentials = CredentialsEntity.builder()
                    .username("bibliotecario")
                    .password(passwordEncoder.encode("admin123"))
                    .enabled(true)
                    .usuario(adminUser)
                    .roles(new HashSet<>(Collections.singletonList(roleBibliotecario)))
                    .build();

            credentialsRepository.save(adminCredentials);
            log.info("Usuario 'bibliotecario' creado con clave 'admin123'");
        }

        // 5. Crear usuario Socio por defecto si no existe
        if (!credentialsRepository.existsByUsername("socio_juan")) {
            UserEntity socioUser = UserEntity.builder()
                    .nombre("Juan")
                    .apellido("Pérez")
                    .email("juan.perez@email.com")
                    .dni("40987654")
                    .telefono("1166778899")
                    .build();

            CredentialsEntity socioCredentials = CredentialsEntity.builder()
                    .username("socio_juan")
                    .password(passwordEncoder.encode("socio123"))
                    .enabled(true)
                    .usuario(socioUser)
                    .roles(new HashSet<>(Collections.singletonList(roleSocio)))
                    .build();

            credentialsRepository.save(socioCredentials);
            log.info("Usuario 'socio_juan' creado con clave 'socio123'");
        }

        log.info("Inicialización de datos de seguridad finalizada con éxito.");
    }
}

