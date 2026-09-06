package com.EduardoMango.Biblioteca.feature.author.service;

import java.util.UUID;

public interface AuthorDeletionValidator {
    boolean hasAssociatedBooks(UUID authorPublicId);
}

