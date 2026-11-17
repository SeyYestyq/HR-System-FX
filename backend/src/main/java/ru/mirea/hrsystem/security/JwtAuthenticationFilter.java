package ru.mirea.hrsystem.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.mirea.hrsystem.repository.UserAccountRepository;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserAccountRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, UserAccountRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                UUID uuid = UUID.fromString(claims.getSubject());
                users.findByUuid(uuid).filter(u -> u.isEnabled()).ifPresent(user -> {
                    UserPrincipal principal = new UserPrincipal(user);
                    var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
