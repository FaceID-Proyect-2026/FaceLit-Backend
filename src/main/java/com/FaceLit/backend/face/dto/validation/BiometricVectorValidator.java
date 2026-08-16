package com.FaceLit.backend.face.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BiometricVectorValidator implements ConstraintValidator<ValidBiometricVector, byte[]> {

    @Override
    public boolean isValid(byte[] value, ConstraintValidatorContext context) {
        return value != null && value.length > 0;
    }
}
