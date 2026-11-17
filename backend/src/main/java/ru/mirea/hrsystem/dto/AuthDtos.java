package ru.mirea.hrsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.mirea.hrsystem.domain.Role;

import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {}

    public record AuthResponse(
            String accessToken,
            String tokenType,
            long expiresInSeconds,
            boolean mustChangePassword,
            MeResponse user
    ) {}

    public record MeResponse(
            UUID uuid,
            String email,
            String fullName,
            Role role,
            boolean mustChangePassword
    ) {}
}
