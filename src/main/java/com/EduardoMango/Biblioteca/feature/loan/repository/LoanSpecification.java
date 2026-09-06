package com.EduardoMango.Biblioteca.feature.loan.repository;

import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LoanSpecification {

    public static Specification<Loan> withFilters(LoanStatus estado, Boolean soloAtrasados, UUID usuarioPublicId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (estado != null) {
                predicates.add(criteriaBuilder.equal(root.get("estado"), estado));
            }

            if (Boolean.TRUE.equals(soloAtrasados)) {
                predicates.add(criteriaBuilder.isNull(root.get("fechaDevolucionEfectiva")));
                predicates.add(criteriaBuilder.lessThan(root.get("fechaDevolucionEsperada"), LocalDate.now()));
            }

            if (usuarioPublicId != null) {
                predicates.add(criteriaBuilder.equal(root.get("usuario").get("publicId"), usuarioPublicId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

