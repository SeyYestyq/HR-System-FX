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
import ru.mirea.hrsystem.dto.EmployerDtos.EmployerRequest;
import ru.mirea.hrsystem.dto.EmployerDtos.EmployerResponse;
import ru.mirea.hrsystem.service.EmployerService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employers")
@SecurityRequirement(name = "bearerAuth")
public class EmployerController {
    private final EmployerService employers;

    public EmployerController(EmployerService employers) {
        this.employers = employers;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN')")
    public List<EmployerResponse> list(@RequestParam(defaultValue = "true") boolean onlyEnabled) {
        return employers.list(onlyEnabled);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployerResponse> create(@Valid @RequestBody EmployerRequest request) {
        return ResponseEntity.status(201).body(employers.create(request));
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployerResponse update(@PathVariable UUID uuid, @Valid @RequestBody EmployerRequest request) {
        return employers.update(uuid, request);
    }
}
