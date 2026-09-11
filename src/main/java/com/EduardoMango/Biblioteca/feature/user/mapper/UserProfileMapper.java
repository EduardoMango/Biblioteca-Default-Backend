package com.EduardoMango.Biblioteca.feature.user.mapper;

import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.user.dto.LoanHistoryResponse;
import com.EduardoMango.Biblioteca.feature.user.dto.UserProfileResponse;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "tituloLibro", source = "libro.titulo")
    LoanHistoryResponse toLoanHistoryResponse(Loan loan);

    List<LoanHistoryResponse> toLoanHistoryResponseList(List<Loan> loans);

    @Mapping(target = "publicId", source = "user.publicId")
    @Mapping(target = "nombre", source = "user.nombre")
    @Mapping(target = "apellido", source = "user.apellido")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "rol", source = "user.rol")
    @Mapping(target = "prestamos", source = "prestamos")
    UserProfileResponse toUserProfileResponse(UserEntity user, List<LoanHistoryResponse> prestamos);
}

