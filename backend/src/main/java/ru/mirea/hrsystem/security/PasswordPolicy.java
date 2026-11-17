package ru.mirea.hrsystem.security;

import org.springframework.stereotype.Component;
import ru.mirea.hrsystem.error.BadRequestException;

@Component
public class PasswordPolicy {
    public void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Пароль должен содержать минимум 8 символов");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BadRequestException("Пароль должен содержать буквы и цифры");
        }
    }
}
