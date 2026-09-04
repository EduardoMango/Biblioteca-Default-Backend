package com.EduardoMango.Biblioteca.repository;

import com.EduardoMango.Biblioteca.model.entity.CredentialsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredentialsRepository extends JpaRepository<CredentialsEntity, Long> {
    Optional<CredentialsEntity> findByUsername(String username);
    Optional<CredentialsEntity> findByRefreshToken(String refreshToken);
    boolean existsByUsername(String username);
}

