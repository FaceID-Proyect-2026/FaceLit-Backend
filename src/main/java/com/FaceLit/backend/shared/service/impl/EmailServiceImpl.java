package com.FaceLit.backend.shared.service.impl;

import org.springframework.stereotype.Service;
import com.FaceLit.backend.shared.service.EmailService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

@Service // Marca esta clase como un servicio de Spring.
// Spring la detecta automáticamente y permite usarla en otras clases.
public class EmailServiceImpl implements EmailService {

    // JavaMailSender es la herramienta que usa Spring
    // para enviar correos electrónicos.

    // private → solo se puede usar dentro de esta clase
    // final → el valor no cambiará después de asignarse
    private final JavaMailSender mailSender;

    // Constructor de la clase.
    // Spring envía automáticamente el JavaMailSender
    // para poder usar el servicio de correos.
    public EmailServiceImpl(JavaMailSender mailSender) {

        // this.mailSender → variable de la clase
        // mailSender → valor recibido en el constructor
        // Aquí se guarda la herramienta de envío de correos.
        this.mailSender = mailSender;
    }

    @Override
    public void sendRecoveryCode(String toEmail, String code) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("FaceLit — Código de recuperación de contraseña");
        message.setText(
                "Hola,\n\n" +
                        "Recibimos una solicitud para restablecer tu contraseña.\n" +
                        "Tu código de recuperación es: " + code + "\n\n" +
                        "Este código expira en 5 minutos.\n" +
                        "Si no solicitaste este cambio, ignora este mensaje y tu contraseña actual seguirá funcionando.\n\n"
                        +
                        "FaceLit");
        mailSender.send(message);
    }
}
