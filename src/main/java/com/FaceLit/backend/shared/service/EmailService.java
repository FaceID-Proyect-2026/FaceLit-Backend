package com.FaceLit.backend.shared.service;

//Interfaz para el service de envio de corre electronico

// Define el método que enviará el código de verificación
// al correo del usuario.
public interface EmailService {

    void sendVerificationCode(String toEmail, String code);
                              // toEmail → correo destino
                              // code → código de verificación enviado al usuario

   //  ESTE  METODO ES PARA EL TOKEN QUE VA A USAR EL ACUDIENTE ( EL RESPONSABLE DEL MENOR DE EDAD)
   // sirve para enviar una solicitud de consentimiento a al acudiente por correo electronico. 
    void sendConsentRequest (String toEmail, String fullName, String token); 

    void sendRecoveryCode(String toEmail, String code);
    


}
