package com.EduardoMango.Biblioteca.feature.user.controller;

import com.EduardoMango.Biblioteca.feature.user.dto.UserAdminResponse;
import com.EduardoMango.Biblioteca.feature.user.dto.UserRoleUpdateRequest;
import com.EduardoMango.Biblioteca.feature.user.dto.UserStatusUpdateRequest;
import com.EduardoMango.Biblioteca.feature.user.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PatchMapping("/{publicId}/estado")
    @PreAuthorize("hasRole('ROLE_BIBLIOTECARIO') or hasRole('BIBLIOTECARIO')")
    public ResponseEntity<UserAdminResponse> updateUserStatus(
            @PathVariable UUID publicId,
            @Valid @RequestBody UserStatusUpdateRequest request,
            Authentication authentication) {
        UserAdminResponse response = userAdminService.updateUserStatus(publicId, request.activo(), authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{publicId}/rol")
    @PreAuthorize("hasRole('ROLE_BIBLIOTECARIO') or hasRole('BIBLIOTECARIO')")
    public ResponseEntity<UserAdminResponse> updateUserRole(
            @PathVariable UUID publicId,
            @Valid @RequestBody UserRoleUpdateRequest request,
            Authentication authentication) {
        UserAdminResponse response = userAdminService.updateUserRole(publicId, request.nuevoRol(), authentication.getName());
        return ResponseEntity.ok(response);
    }
}

