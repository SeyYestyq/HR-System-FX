package ru.mirea.hrsystem.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.hrsystem.dto.PageResponse;
import ru.mirea.hrsystem.dto.UserDtos.CreateUserRequest;
import ru.mirea.hrsystem.dto.UserDtos.CreatedUserResponse;
import ru.mirea.hrsystem.dto.UserDtos.UpdateUserRequest;
import ru.mirea.hrsystem.dto.UserDtos.UserResponse;
import ru.mirea.hrsystem.service.UserAdminService;
import ru.mirea.hrsystem.web.PageableFactory;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserAdminService users;
    private final PageableFactory pageableFactory;

    public AdminUserController(UserAdminService users, PageableFactory pageableFactory) {
        this.users = users;
        this.pageableFactory = pageableFactory;
    }

    @GetMapping
    public PageResponse<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return PageResponse.from(users.list(pageableFactory.users(page, size, sort)));
    }

    @PostMapping
    public ResponseEntity<CreatedUserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(201).body(users.create(request));
    }

    @PutMapping("/{uuid}")
    public UserResponse update(@PathVariable UUID uuid, @Valid @RequestBody UpdateUserRequest request) {
        return users.update(uuid, request);
    }

    @PostMapping("/{uuid}/reset-password")
    public CreatedUserResponse resetPassword(@PathVariable UUID uuid) {
        return users.resetPassword(uuid);
    }
}
