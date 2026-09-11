package com.EduardoMango.Biblioteca.feature.loan;

import com.EduardoMango.Biblioteca.feature.loan.domain.Loan;
import com.EduardoMango.Biblioteca.feature.loan.dto.BookLoanSummary;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanResponse;
import com.EduardoMango.Biblioteca.feature.loan.dto.LoanSupervisionResponse;
import com.EduardoMango.Biblioteca.feature.loan.dto.UserLoanSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    @Mapping(target = "libroIsbn", source = "libro.isbn")
    @Mapping(target = "libroTitulo", source = "libro.titulo")
    @Mapping(target = "usuarioPublicId", source = "usuario.publicId")
    LoanResponse toLoanResponse(Loan loan);

    @Mapping(target = "usuario", source = "loan", qualifiedByName = "toUserSummary")
    @Mapping(target = "libro", source = "loan", qualifiedByName = "toBookSummary")
    @Mapping(target = "diasAtraso", source = "loan", qualifiedByName = "calculateDiasAtraso")
    LoanSupervisionResponse toLoanSupervisionResponse(Loan loan);

    @Named("toUserSummary")
    default UserLoanSummary toUserSummary(Loan loan) {
        if (loan == null || loan.getUsuario() == null) return null;
        return new UserLoanSummary(
                loan.getUsuario().getPublicId(),
                loan.getUsuario().getNombre(),
                loan.getUsuario().getEmail()
        );
    }

    @Named("toBookSummary")
    default BookLoanSummary toBookSummary(Loan loan) {
        if (loan == null || loan.getLibro() == null) return null;
        return new BookLoanSummary(
                loan.getLibro().getIsbn(),
                loan.getLibro().getTitulo()
        );
    }

    @Named("calculateDiasAtraso")
    default Long calculateDiasAtraso(Loan loan) {
        if (loan == null || loan.getFechaDevolucionEsperada() == null) return 0L;
        LocalDate compareDate = loan.getFechaDevolucionEfectiva() != null
                ? loan.getFechaDevolucionEfectiva()
                : LocalDate.now();

        if (compareDate.isAfter(loan.getFechaDevolucionEsperada())) {
            return ChronoUnit.DAYS.between(loan.getFechaDevolucionEsperada(), compareDate);
        }
        return 0L;
    }
}
