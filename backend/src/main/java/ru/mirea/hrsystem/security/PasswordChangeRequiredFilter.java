package ru.mirea.hrsystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.mirea.hrsystem.dto.ApiError;
import ru.mirea.hrsystem.web.RequestMetadata;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOWED = Set.of(
            "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
            "/api/auth/me", "/api/auth/change-password"
    );

    private final ObjectMapper objectMapper;

    public PasswordChangeRequiredFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal
                && principal.isMustChangePassword()) {
            String path = request.getRequestURI();
            if (path.startsWith("/api/") && !ALLOWED.contains(path)) {
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), new ApiError(
                        Instant.now(), 403, "PASSWORD_CHANGE_REQUIRED",
                        "Необходимо сменить временный пароль", path,
                        response.getHeader(RequestMetadata.CORRELATION_HEADER), Map.of()));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
