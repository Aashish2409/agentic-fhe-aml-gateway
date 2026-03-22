package com.aml.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — Centralised REST error handling.
 *
 * <p>Translates exceptions into structured JSON error responses.
 * Keeps the controller clean of try-catch blocks.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation failures (e.g., missing required fields).
     * Returns HTTP 400 with a map of field → error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value",
                        (a, b) -> a  // keep first if duplicate field
                ));

        log.warn("GlobalExceptionHandler: validation error — {}", fieldErrors);

        return ResponseEntity.badRequest().body(Map.of(
                "status", "BAD_REQUEST",
                "errors", fieldErrors,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Handles illegal argument exceptions (e.g., invalid ciphertext format).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("GlobalExceptionHandler: illegal argument — {}", ex.getMessage());

        return ResponseEntity.badRequest().body(Map.of(
                "status", "BAD_REQUEST",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Catch-all handler for unexpected errors.
     * Returns HTTP 500 without exposing internal details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {
        log.error("GlobalExceptionHandler: unexpected error — {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "INTERNAL_ERROR",
                "message", "An unexpected error occurred. Please contact support.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
