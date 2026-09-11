package com.EduardoMango.Biblioteca.feature.user.mapper;

import com.EduardoMango.Biblioteca.feature.user.dto.UserAdminResponse;
import com.EduardoMango.Biblioteca.feature.user.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserAdminMapper {

    UserAdminResponse toUserAdminResponse(UserEntity user);
}

