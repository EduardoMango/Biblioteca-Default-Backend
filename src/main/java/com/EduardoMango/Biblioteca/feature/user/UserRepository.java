package com.EduardoMango.Biblioteca.feature.user;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByPublicId(UUID publicId);
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByDni(String dni);
    boolean existsByEmail(String email);
}

