package com.EduardoMango.Biblioteca.repository;

import com.EduardoMango.Biblioteca.model.entity.PermitEntity;
import com.EduardoMango.Biblioteca.model.enums.Permits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermitRepository extends JpaRepository<PermitEntity, Long> {
    Optional<PermitEntity> findByPermit(Permits permit);
}

