package com.FaceLit.backend.shared.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.auth.exception.AcceptanceTermsException;
import com.FaceLit.backend.auth.exception.RegisterException;

@RestController // esta anotacion funciona para que se escuchen TODOS los errores que ocurre en
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

    // Captura Error del registro - email duplicado, documento duplicado etc.
    @ExceptionHandler(RegisterException.class)
    public ResponseEntity<Map<String, String>> handleRegistro(
            RegisterException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));

    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {

        return ResponseEntity
                .internalServerError()
                .body(Map.of("error", "Error interno del servidor"));
    }

    @ExceptionHandler(AcceptanceTermsException.class)
    public ResponseEntity<Map<String, String>> handleTerminos(AcceptanceTermsException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
