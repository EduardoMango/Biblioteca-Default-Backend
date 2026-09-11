package com.EduardoMango.Biblioteca.feature.user.service;

import com.EduardoMango.Biblioteca.exception.ResourceNotFoundException;
import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.repository.LoanRepository;
import com.EduardoMango.Biblioteca.feature.user.dto.LoanHistoryResponse;
import com.EduardoMango.Biblioteca.feature.user.dto.UserProfileResponse;
import com.EduardoMango.Biblioteca.feature.user.mapper.UserProfileMapper;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import com.EduardoMango.Biblioteca.feature.auth.repository.CredentialsRepository;
import com.EduardoMango.Biblioteca.feature.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final LoanRepository loanRepository;
    private final UserProfileMapper userProfileMapper;

    public UserProfileResponse getUserProfile(String principalName) {
        UserEntity user = findUserByPrincipal(principalName);
        List<Loan> loans = loanRepository.findByUsuarioOrderByFechaPrestamoDesc(user);
        List<LoanHistoryResponse> loanResponses = userProfileMapper.toLoanHistoryResponseList(loans);
        return userProfileMapper.toUserProfileResponse(user, loanResponses);
    }

    public Page<LoanHistoryResponse> getUserLoans(String principalName, Pageable pageable) {
        UserEntity user = findUserByPrincipal(principalName);
        Page<Loan> loansPage = loanRepository.findByUsuarioOrderByFechaPrestamoDesc(user, pageable);
        return loansPage.map(userProfileMapper::toLoanHistoryResponse);
    }

    private UserEntity findUserByPrincipal(String principalName) {
        return credentialsRepository.findByUsername(principalName)
                .map(credentials -> credentials.getUsuario())
                .or(() -> userRepository.findByEmail(principalName))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + principalName));
    }
}

