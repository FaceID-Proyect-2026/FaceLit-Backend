package com.FaceLit.backend.auth.exception;

public class RegisterException extends RuntimeException {
    // esta clse sera un tipo de error, RuntimeException ya existe en Java.
    // Execiones
    public RegisterException(String message) { // sirve para lanzar error
        super(message);   //hereda su coportamiento

    }

}
