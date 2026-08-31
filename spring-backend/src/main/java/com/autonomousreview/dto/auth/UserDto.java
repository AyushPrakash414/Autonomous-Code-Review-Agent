package com.autonomousreview.dto.auth;

import com.autonomousreview.model.AuthProvider;
import com.autonomousreview.model.Role;
import com.autonomousreview.model.User;

import java.time.Instant;

public record UserDto(
        String id,
        String email,
        String fullName,
        Role role,
        AuthProvider authProvider,
        String avatarUrl,
        Instant createdAt
) {
    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getAuthProvider(),
                user.getAvatarUrl(),
                user.getCreatedAt()
        );
    }
}
