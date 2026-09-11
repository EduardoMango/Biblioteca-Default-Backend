package com.EduardoMango.Biblioteca.feature.author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByPublicId(UUID publicId);

    @Query("SELECT a FROM Author a WHERE " +
            "(:q IS NULL OR :q = '' OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.apellido) LIKE LOWER(CONCAT('%', :q, '%'))) AND " +
            "(:nacionalidad IS NULL OR :nacionalidad = '' OR LOWER(a.nacionalidad) LIKE LOWER(CONCAT('%', :nacionalidad, '%')))")
    List<Author> searchAuthors(@Param("q") String q, @Param("nacionalidad") String nacionalidad);

    void deleteByPublicId(UUID publicId);
}

