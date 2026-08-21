package com.FaceLit.backend.shared.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.FaceLit.backend.auth.exception.AcceptanceTermsException;
import com.FaceLit.backend.auth.exception.PasswordRecoveryException;
import com.FaceLit.backend.auth.exception.RegisterException;
import com.FaceLit.backend.face.exception.DeviceException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidacion(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(
                        error.getField(),
                        error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errores);
    }

    // Captura errores del registro
    @ExceptionHandler(RegisterException.class)
    public ResponseEntity<Map<String, String>> handleRegistro(
            RegisterException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    // Captura errores de dispositivos
    @ExceptionHandler(DeviceException.class)
    public ResponseEntity<Map<String, String>> handleDeviceException(
            DeviceException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(AcceptanceTermsException.class)
    public ResponseEntity<Map<String, String>> handleTerminos(
            AcceptanceTermsException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(PasswordRecoveryException.class)
    public ResponseEntity<Map<String, String>> handlePasswordRecovery(
            PasswordRecoveryException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {

        return ResponseEntity
                .internalServerError()
                .body(Map.of("message", "Error interno del servidor"));
    }
}
