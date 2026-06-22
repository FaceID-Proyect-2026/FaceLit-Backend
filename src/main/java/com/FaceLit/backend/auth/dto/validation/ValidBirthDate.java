package com.FaceLit.backend.auth.dto.validation;

// ValidBirthDate es una anotacion que personaliza las validciones, 
// en este caso validaciones den edad, 
// estas como ( no aceptar fechas futuras, minimo de 18 años, y que no sea muy antigua)

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// FIELD = atributo de una clase (variable dentro de una clase)
@Target(ElementType.FIELD) // esta anotacion solo se puede usar en atributtos (VARIABLES) NO EN METODOS

@Retention(RetentionPolicy.RUNTIME) // Esta anotacion funciona Solo cuando el programa se este ejecutando, // le la
                                    // clase USER y ejecuta . Ejecuta BirthDateValidator

@Constraint(validatedBy =  BirthDateValidator.class) // aqui decimos que la logica de validacion esta en la clases
                                                    // BirthDateValidator
                                                    // es como redirigirse a esta clase y validar aqui  BirthDateValidator
public @interface ValidBirthDate { // esta es la anotacion personalizada para las validaciones de edad

    // Mensajes por defecto si no se especifica uno
    
    String  message() default "La fecha de nacimiento de es valida"; // mensaje que se muestra cuando falla la validacion
    Class<?>[] groups() default{};  // un arreglo (lista) de clases para agrupar validaciones, por ahora np hay grupos
    Class<? extends Payload>[] payload() default {};  // ingresa metadatos en las validaciones ( datos sobre datos)

    // metadatos = datos sobre datos, informacion adicional
    // default{}; -- esto es por que el metodo noo se va a usar todavia
}
