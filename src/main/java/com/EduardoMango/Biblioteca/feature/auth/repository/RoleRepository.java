package com.EduardoMango.Biblioteca.feature.auth.repository;

import com.EduardoMango.Biblioteca.feature.auth.domain.RoleEntity;
import com.EduardoMango.Biblioteca.feature.auth.domain.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByRole(Roles role);
}

