package org.codibly.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.codibly.dto.response.ErrorResponse;
import org.codibly.exception.GenerationProviderConnectionException;
import org.codibly.exception.NoGenerationFoundExcepion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;

@Slf4j
@RestControllerAdvice
public class GenerationProviderExceptionHandler {

    @ExceptionHandler(GenerationProviderConnectionException.class)
    public ResponseEntity<ErrorResponse> handleConnectionException(GenerationProviderConnectionException ex,
                                                                         HttpServletRequest request) {
        log.warn("Warn occurred: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_GATEWAY,
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(errorResponse);
    }

    @ExceptionHandler(NoGenerationFoundExcepion.class)
    public ResponseEntity<ErrorResponse> handleNoGenerationFound(NoGenerationFoundExcepion ex,
                                                                 HttpServletRequest request) {
        log.warn("No generation data found: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    private ErrorResponse buildErrorResponse(
            HttpStatus status,
            String message,
            String path) {
        return new ErrorResponse(
                ZonedDateTime.now(),
                status.value(),
                status.name(),
                message,
                path
        );
    }
}
