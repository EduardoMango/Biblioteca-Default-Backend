package com.EduardoMango.Biblioteca.feature.category.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnMissingBean(name = "bookCategoryDeletionValidator")
public class DefaultCategoryDeletionValidator implements CategoryDeletionValidator {
    @Override
    public boolean hasAssociatedBooks(UUID categoryPublicId) {
        return false;
    }
}

