package ru.mirea.hrsystem.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.mirea.hrsystem.domain.UserAccount;
import ru.mirea.hrsystem.error.AuthenticationFailedException;
import ru.mirea.hrsystem.error.NotFoundException;
import ru.mirea.hrsystem.repository.UserAccountRepository;

@Service
public class CurrentUserService {
    private final UserAccountRepository users;

    public CurrentUserService(UserAccountRepository users) {
        this.users = users;
    }

    public UserPrincipal requirePrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationFailedException("Требуется авторизация");
        }
        return principal;
    }

    public UserAccount requireEntity() {
        UserPrincipal principal = requirePrincipal();
        return users.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }
}
