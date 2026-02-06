package com.tayperformance.mapper;

import com.tayperformance.dto.user.UserDto;
import com.tayperformance.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserDto toDto(User u) {
        if (u == null) return null;

        return UserDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .role(u.getRole().name())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .displayName(u.getDisplayName())
                .build();
    }
}