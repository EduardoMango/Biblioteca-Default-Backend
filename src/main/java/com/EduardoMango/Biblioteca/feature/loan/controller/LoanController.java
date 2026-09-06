package com.EduardoMango.Biblioteca.feature.loan.controller;

import com.EduardoMango.Biblioteca.feature.loan.domain.LoanStatus;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanCreateRequest;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanResponse;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanSupervisionResponse;
import com.EduardoMango.Biblioteca.feature.loan.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/prestamos")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LoanResponse> createLoan(
            @Valid @RequestBody LoanCreateRequest request,
            Authentication authentication) {
        LoanResponse response = loanService.createLoan(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{publicId}/devolucion")
    @PreAuthorize("hasRole('ROLE_BIBLIOTECARIO') or hasRole('BIBLIOTECARIO')")
    public ResponseEntity<LoanResponse> returnLoan(@PathVariable UUID publicId) {
        LoanResponse response = loanService.returnLoan(publicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/supervision")
    @PreAuthorize("hasRole('ROLE_BIBLIOTECARIO') or hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Page<LoanSupervisionResponse>> getLoansForSupervision(
            @RequestParam(required = false) LoanStatus estado,
            @RequestParam(required = false) Boolean soloAtrasados,
            @RequestParam(required = false) UUID usuarioPublicId,
            @PageableDefault(size = 10, sort = "fechaDevolucionEsperada", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<LoanSupervisionResponse> page = loanService.getLoansForSupervision(estado, soloAtrasados, usuarioPublicId, pageable);
        return ResponseEntity.ok(page);
    }
}

