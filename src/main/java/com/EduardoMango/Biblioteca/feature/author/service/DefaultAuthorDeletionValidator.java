package com.EduardoMango.Biblioteca.feature.author.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnMissingBean(name = "bookAuthorDeletionValidator")
public class DefaultAuthorDeletionValidator implements AuthorDeletionValidator {
    @Override
    public boolean hasAssociatedBooks(UUID authorPublicId) {
        return false;
    }
}

