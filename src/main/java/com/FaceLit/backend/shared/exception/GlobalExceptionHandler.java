package com.FaceLit.backend.shared.exception;

import java.util.HashMap;
import java.util.Map;

// Imports nuevos en GlobalExceptionHandler.java
import com.FaceLit.backend.academic.exception.ChipException;
import com.FaceLit.backend.academic.exception.ProgramException;
import com.FaceLit.backend.academic.exception.UserChipException;
import com.FaceLit.backend.environments.exception.EnvironmentException;
import com.FaceLit.backend.environments.exception.ChipEnvironmentException;
import com.FaceLit.backend.schedule.exception.ScheduleException;
import com.FaceLit.backend.auth.exception.LoginException;
import org.springframework.http.HttpStatus;
import com.FaceLit.backend.schedule.exception.ScheduleExceptionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.FaceLit.backend.auth.exception.PasswordRecoveryException;
import com.FaceLit.backend.auth.exception.AcceptanceTermsException;
import com.FaceLit.backend.auth.exception.RegisterException;
import com.FaceLit.backend.auth.exception.UserManagementException;

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

    @ExceptionHandler(RegisterException.class)
    public ResponseEntity<Map<String, String>> handleRegistro(RegisterException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(AcceptanceTermsException.class)
    public ResponseEntity<Map<String, String>> handleTerminos(AcceptanceTermsException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(PasswordRecoveryException.class)
    public ResponseEntity<Map<String, String>> handlePasswordRecovery(PasswordRecoveryException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(LoginException.class)
    public ResponseEntity<Map<String, String>> handleLogin(LoginException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(UserManagementException.class)
    public ResponseEntity<Map<String, String>> handleUserManagement(UserManagementException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ScheduleExceptionException.class)
    public ResponseEntity<Map<String, String>> handleScheduleException(ScheduleExceptionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(EnvironmentException.class)
    public ResponseEntity<Map<String, String>> handleEnvironment(EnvironmentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ChipEnvironmentException.class)
    public ResponseEntity<Map<String, String>> handleChipEnvironment(ChipEnvironmentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ChipException.class)
    public ResponseEntity<Map<String, String>> handleChip(ChipException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ProgramException.class)
    public ResponseEntity<Map<String, String>> handleProgram(ProgramException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(UserChipException.class)
    public ResponseEntity<Map<String, String>> handleUserChip(UserChipException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ScheduleException.class)
    public ResponseEntity<Map<String, String>> handleSchedule(ScheduleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of("message", "Error interno del servidor"));
    }
}

