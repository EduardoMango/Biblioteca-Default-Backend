package com.EduardoMango.Biblioteca.feature.auth.repository;

import com.EduardoMango.Biblioteca.feature.auth.domain.PermitEntity;
import com.EduardoMango.Biblioteca.feature.auth.domain.Permits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermitRepository extends JpaRepository<PermitEntity, Long> {
    Optional<PermitEntity> findByPermit(Permits permit);
}

