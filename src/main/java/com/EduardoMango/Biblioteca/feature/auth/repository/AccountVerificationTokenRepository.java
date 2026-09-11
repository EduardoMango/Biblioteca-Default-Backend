package com.EduardoMango.Biblioteca.feature.auth.repository;

import com.EduardoMango.Biblioteca.feature.auth.domain.AccountVerificationToken;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountVerificationTokenRepository extends JpaRepository<AccountVerificationToken, Long> {
    Optional<AccountVerificationToken> findByToken(String token);
    Optional<AccountVerificationToken> findByPublicId(UUID publicId);
    List<AccountVerificationToken> findByUserAndUsedFalse(UserEntity user);
}

