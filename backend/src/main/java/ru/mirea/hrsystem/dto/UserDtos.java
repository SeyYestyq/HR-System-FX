package ru.mirea.hrsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.mirea.hrsystem.domain.Role;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {
    private UserDtos() {}

    public record UserResponse(
            UUID uuid,
            String email,
            String fullName,
            Role role,
            boolean enabled,
            boolean mustChangePassword,
            Instant createdAt
    ) {}

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 2, max = 150) String fullName,
            @NotNull Role role
    ) {}

    public record UpdateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 2, max = 150) String fullName,
            @NotNull Role role,
            boolean enabled
    ) {}

    public record CreatedUserResponse(UserResponse user, String temporaryPassword) {}
}
