package com.FaceLit.backend.shared.util;

import java.time.LocalDate;
import java.time.Period;

import com.FaceLit.backend.shared.constants.AppConstants;

// Centraliza el cálculo de edad y minoría
public final class AgeUtils {

    private AgeUtils() {
    }

    public static int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    // Menor de edad = <18 años O documento tipo TI
    // (la TI en Colombia es exclusiva de menores, por eso se valida junto con la
    // edad)
    public static boolean isMinor(LocalDate birthDate, String documentAbbreviation) {
        int age = calculateAge(birthDate);
        return age < AppConstants.LEGAL_AGE || "TI".equals(documentAbbreviation);
    }

}
