package com.EduardoMango.Biblioteca.feature.category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByPublicId(UUID publicId);

    Optional<Category> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndPublicIdNot(String nombre, UUID publicId);

    void deleteByPublicId(UUID publicId);
}

