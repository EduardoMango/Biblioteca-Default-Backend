package com.EduardoMango.Biblioteca.feature.auth.repository;

import com.EduardoMango.Biblioteca.feature.auth.domain.CredentialsEntity;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CredentialsRepository extends JpaRepository<CredentialsEntity, Long> {
    Optional<CredentialsEntity> findByUsername(String username);
    Optional<CredentialsEntity> findByRefreshToken(String refreshToken);
    boolean existsByUsername(String username);
    Optional<CredentialsEntity> findByUsuario(UserEntity usuario);
    Optional<CredentialsEntity> findByUsuario_PublicId(UUID publicId);
}

