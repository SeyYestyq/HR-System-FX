package ru.mirea.hrsystem.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.hrsystem.dto.AuditDtos.AuditResponse;
import ru.mirea.hrsystem.dto.PageResponse;
import ru.mirea.hrsystem.service.AuditService;
import ru.mirea.hrsystem.web.PageableFactory;

@RestController
@RequestMapping("/api/admin/audit")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {
    private final AuditService audit;
    private final PageableFactory pageableFactory;

    public AdminAuditController(AuditService audit, PageableFactory pageableFactory) {
        this.audit = audit;
        this.pageableFactory = pageableFactory;
    }

    @GetMapping
    public PageResponse<AuditResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return PageResponse.from(audit.search(q, pageableFactory.audit(page, size, sort)));
    }
}
