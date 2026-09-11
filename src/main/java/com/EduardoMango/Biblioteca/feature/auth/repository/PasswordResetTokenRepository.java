package com.EduardoMango.Biblioteca.feature.auth.repository;

import com.EduardoMango.Biblioteca.feature.auth.domain.PasswordResetToken;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByPublicId(UUID publicId);
    List<PasswordResetToken> findByUserAndUsedFalse(UserEntity user);
}

