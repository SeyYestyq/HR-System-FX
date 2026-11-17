package ru.mirea.hrsystem.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.hrsystem.dto.AuthDtos.AuthResponse;
import ru.mirea.hrsystem.dto.AuthDtos.ChangePasswordRequest;
import ru.mirea.hrsystem.dto.AuthDtos.LoginRequest;
import ru.mirea.hrsystem.dto.AuthDtos.MeResponse;
import ru.mirea.hrsystem.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return auth.login(request, httpRequest, httpResponse);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        return auth.refresh(request, response);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        auth.logout(request, response);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public MeResponse me() {
        return auth.me();
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    public MeResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return auth.changePassword(request);
    }
}
