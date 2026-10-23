
package org.example;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle invalid banking operations
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidInput(
            IllegalArgumentException exception
    ) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "error", exception.getMessage()
                ));
    }

    // Handle database errors
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Map<String, String>> handleDatabaseError(
            SQLException exception
    ) {

        // SQLite constraint violation
        if (exception.getErrorCode() == 19) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "A database constraint was violated."
                    ));
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "A database operation failed."
                ));
    }
}
