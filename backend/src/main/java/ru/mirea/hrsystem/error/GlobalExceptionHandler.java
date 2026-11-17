package ru.mirea.hrsystem.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.mirea.hrsystem.dto.ApiError;
import ru.mirea.hrsystem.web.RequestMetadata;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Некорректные данные запроса", request, fields);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> badRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler({AuthenticationFailedException.class, BadCredentialsException.class})
    public ResponseEntity<ApiError> unauthorized(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Недостаточно прав для операции", request, Map.of());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler({ConflictException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiError> conflict(RuntimeException ex, HttpServletRequest request) {
        String message = ex instanceof ConflictException
                ? ex.getMessage()
                : "Конфликт версий: запись изменена другим пользователем";
        return build(HttpStatus.CONFLICT, "CONFLICT", message, request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> other(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Внутренняя ошибка сервера", request, Map.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message,
                                           HttpServletRequest request, Map<String, String> fields) {
        ApiError body = new ApiError(Instant.now(), status.value(), error, message,
                request.getRequestURI(), RequestMetadata.correlationId(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
