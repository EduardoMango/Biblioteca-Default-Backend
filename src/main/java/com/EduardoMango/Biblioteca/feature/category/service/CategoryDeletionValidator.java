package com.EduardoMango.Biblioteca.feature.category.service;

import java.util.UUID;

public interface CategoryDeletionValidator {
    boolean hasAssociatedBooks(UUID categoryPublicId);
}

