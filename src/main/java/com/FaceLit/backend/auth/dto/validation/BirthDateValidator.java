package com.FaceLit.backend.auth.dto.validation;

import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Period;

// ContrainValidatos recibe  dos tipos 
// 1 - ValidBirthDate - la anotacion que creamos
// 2 - LocalDate - el tipo del campo que va a validar
public class BirthDateValidator implements ConstraintValidator<ValidBirthDate, LocalDate> {

    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {

        // Si la fecha de nacimiento es null, @NotNUll ya lo maneja - aqui no hacemos
        // nada
        if (birthDate == null)
            return true;

        // Calcula la edad exacta
        // Period.between(fechaInicio, fechaFin) lo que hace es calcular la diferencia
        // entre dos fechas
        int age = Period.between(birthDate, LocalDate.now()).getYears();

        // Desactiva el mensaje por defecto para poner los mensajes personalizados
        context.disableDefaultConstraintViolation(); // Es para controlar qué mensaje exacto recibe el frontend

        // si la edad es menor a 8 años, se mostrara el mensaje "El usuario debe tener
        // al menos 8 años", y la validacion Falla (return false), error
        if (age < 8) {
            context.buildConstraintViolationWithTemplate("El usuario debe tener al menos 8 años")
                    .addConstraintViolation();
            return false;

        }

        // si la edad es mayor a 100 años, se mostrara el mensaje "La fecha de
        // nacimiento no es valida", y la validacion Falla (return false), error
        if (age > 100) {
            context.buildConstraintViolationWithTemplate("La fecha de nacimiento no es valida")
                    .addConstraintViolation();
            return false;
        }

        // Edad entre 8 y 100 - valido
        return true; 

    }

}
