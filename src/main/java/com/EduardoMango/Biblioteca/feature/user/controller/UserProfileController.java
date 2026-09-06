package com.EduardoMango.Biblioteca.feature.user.controller;

import com.EduardoMango.Biblioteca.feature.user.dto.LoanHistoryResponse;
import com.EduardoMango.Biblioteca.feature.user.dto.UserProfileResponse;
import com.EduardoMango.Biblioteca.feature.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        UserProfileResponse response = userProfileService.getUserProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/prestamos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LoanHistoryResponse>> getMyLoans(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "fechaPrestamo", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LoanHistoryResponse> response = userProfileService.getUserLoans(authentication.getName(), pageable);
        return ResponseEntity.ok(response);
    }
}

