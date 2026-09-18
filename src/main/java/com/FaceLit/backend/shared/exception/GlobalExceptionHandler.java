package com.FaceLit.backend.shared.exception;

import java.util.HashMap;
import java.util.Map;

// Imports nuevos en GlobalExceptionHandler.java
import com.FaceLit.backend.auth.exception.LoginException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.FaceLit.backend.auth.exception.PasswordRecoveryException;
import com.FaceLit.backend.auth.exception.UserManagementException;
import com.FaceLit.backend.auth.exception.ChangePasswordException;
import com.FaceLit.backend.academic.exception.AcademicException;

@RestControllerAdvice // esta anotacion funciona para que se escuchen TODOS los errores que ocurre en
// los controller
public class GlobalExceptionHandler { // esta clase es para que capture errores feos y devuelve un JSON bonito

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidacion(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errores);
    }

    @ExceptionHandler(PasswordRecoveryException.class)
    public ResponseEntity<Map<String, String>> handlePasswordRecovery(PasswordRecoveryException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ChangePasswordException.class)
    public ResponseEntity<Map<String, String>> handleChangePassword(ChangePasswordException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(AcademicException.class)
    public ResponseEntity<Map<String, String>> handleAcademic(AcademicException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(LoginException.class)
    public ResponseEntity<Map<String, String>> handleLogin(LoginException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(UserManagementException.class)
    public ResponseEntity<Map<String, String>> handleUserManagement(UserManagementException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of("message", "Error interno del servidor"));
    }
}

