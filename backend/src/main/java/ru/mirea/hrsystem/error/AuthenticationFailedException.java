package ru.mirea.hrsystem.error;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) { super(message); }
}
