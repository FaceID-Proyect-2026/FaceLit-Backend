package com.FaceLit.backend.auth.exception;

// esta clse sera un tipo de error, RuntimeException ya existe en Java.
// Execiones
public class AcceptanceTermsException extends RuntimeException  {

    public AcceptanceTermsException (String message) { // ayuda a leer el error
        super(message); // Heredar el comportamiento
    }

}
