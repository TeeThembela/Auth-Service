package com.teetech.authservice.exception;

import com.teetech.authservice.dto.error.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final Set<String> CONFLICT_CODES = Set.of(
            "EMAIL_ALREADY_EXISTS"
    );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("DTO validation failed: {} field error(s)",
                ex.getBindingResult().getFieldErrorCount());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(new ErrorResponse(
                "Validation Failed",
                HttpStatus.BAD_REQUEST.value(),
                "The data provided is invalid",
                request.getRequestURI(),
                Instant.now(),
                errors
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn("Type mismatch at {}: parameter '{}' received value '{}'",
                request.getRequestURI(), ex.getName(), ex.getValue());

        String message = String.format(
                "Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());

        return ResponseEntity.badRequest().body(new ErrorResponse(
                "Invalid Parameter",
                HttpStatus.BAD_REQUEST.value(),
                message,
                request.getRequestURI(),
                Instant.now(),
                null
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Unreadable request body at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(new ErrorResponse(
                "Malformed Request",
                HttpStatus.BAD_REQUEST.value(),
                "Request body is missing or malformed",
                request.getRequestURI(),
                Instant.now(),
                null
        ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.warn("No resource found for {} {}",
                ex.getHttpMethod(), ex.getResourcePath());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                "Not Found",
                HttpStatus.NOT_FOUND.value(),
                String.format("No resource found for %s /%s",
                        ex.getHttpMethod(), ex.getResourcePath()),
                request.getRequestURI(),
                Instant.now(),
                null
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String message = ex.getMessage() != null && ex.getMessage().contains("username")
                ? "An account with this email already exists"
                : "A database constraint was violated";

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "Conflict",
                HttpStatus.CONFLICT.value(),
                message,
                request.getRequestURI(),
                Instant.now(),
                null
        ));
    }
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            JwtException ex, HttpServletRequest request) {

        log.warn("JWT exception at {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                "Unauthorized",
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid or expired token",
                request.getRequestURI(),
                Instant.now(),
                null
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("Authentication failed at {}: {}",
                request.getRequestURI(), ex.getClass().getSimpleName());

        String message = switch (ex) {
            case BadCredentialsException e -> "Invalid email or password";
            case DisabledException e -> "Account is disabled";
            case LockedException e -> "Account is locked";
            case AccountExpiredException e -> "Account has expired";
            case CredentialsExpiredException e -> "Credentials have expired";
            default -> "Authentication failed";
        };

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                "Unauthorized",
                HttpStatus.UNAUTHORIZED.value(),
                message,
                request.getRequestURI(),
                Instant.now(),
                null
        ));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {

        log.warn("Invalid token at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                "Unauthorized",
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now(),
                null
        ));
    }

    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<ErrorResponse> handleAuthServiceException(
            AuthServiceException ex, HttpServletRequest request) {

        log.warn("Service exception: code={}, message={}",
                ex.getErrorCode(), ex.getMessage());

        Map<String, String> details = new HashMap<>();
        details.put(ex.getErrorCode(), ex.getMessage());

        HttpStatus status = CONFLICT_CODES.contains(ex.getErrorCode())
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status).body(new ErrorResponse(
                status.getReasonPhrase(),
                status.value(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now(),
                details
        ));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingCookie(
            MissingRequestCookieException ex, HttpServletRequest request) {

        log.warn("Missing cookie at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                "Unauthorized",
                HttpStatus.UNAUTHORIZED.value(),
                "Refresh token missing",
                request.getRequestURI(),
                Instant.now(),
                null
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Invalid argument at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(new ErrorResponse(
                "Invalid Argument",
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now(),
                null
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception at {}: {}",
                request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI(),
                Instant.now(),
                null
        ));
    }
}
