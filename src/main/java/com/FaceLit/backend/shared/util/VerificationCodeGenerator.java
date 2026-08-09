package com.FaceLit.backend.shared.util;

import java.util.Random;
import org.springframework.stereotype.Component;
import com.FaceLit.backend.shared.constants.AppConstants;

//Reemplaza las 3 implementaciones duplicadas de generación de código.
@Component
public class VerificationCodeGenerator {

    private final Random random = new Random();

    // Genera un código de 6 dígitos con ceros a la izquierda si aplica
    // Ejemplo: 483 -> "000483"

    public String generate() {
        return String.format(
                "%0" + AppConstants.VERIFICATION_CODE_LENGTH + "d",
                random.nextInt(AppConstants.VERIFICATION_CODE_MAX));
    }

}
