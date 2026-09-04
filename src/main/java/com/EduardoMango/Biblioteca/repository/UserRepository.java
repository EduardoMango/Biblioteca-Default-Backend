package com.EduardoMango.Biblioteca.repository;

import com.EduardoMango.Biblioteca.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByDni(String dni);
    boolean existsByEmail(String email);
}

