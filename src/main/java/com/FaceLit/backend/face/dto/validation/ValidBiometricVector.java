package com.FaceLit.backend.face.dto.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BiometricVectorValidator.class)
public @interface ValidBiometricVector {

    String message() default "El vector biométrico es obligatorio y no puede estar vacío";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
