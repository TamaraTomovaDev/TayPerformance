package com.tayperformance.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String role;
    private boolean active;
    private String displayName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
