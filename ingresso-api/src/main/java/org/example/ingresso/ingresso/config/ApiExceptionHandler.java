package org.example.ingresso.ingresso.config;

import org.example.ingresso.ingresso.service.exception.ApplicationServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApplicationServiceException.class)
    public ResponseEntity<Map<String, String>> handleApplicationServiceException(
        ApplicationServiceException exception
    ) {
        HttpStatus status = switch (exception.getCode()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
        };
        return ResponseEntity.status(status).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("message", exception.getMessage()));
    }
}
