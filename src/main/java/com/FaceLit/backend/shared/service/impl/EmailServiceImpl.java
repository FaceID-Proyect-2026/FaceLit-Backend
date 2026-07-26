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
    public void sendVerificationCode(String toEmail, String code) {

        // Se crea un objeto que representará el correo electrónico.
        SimpleMailMessage message = new SimpleMailMessage();

        // Correo del destinatario al que se enviará el mensaje.
        message.setTo(toEmail);
        // Asunto o título del correo.
        message.setSubject("FaceLit — Código de verificación");
        // Contenido del correo.
        message.setText(
                "Hola,\n\n" +
                        "Tu código de verificación es: " + code + "\n\n" +
                        "Este código expira en 5 minutos.\n" +
                        "Si no solicitaste este código, ignora este mensaje.\n\n" +
                        "FaceLit");
        // Envía el correo usando JavaMailSender.
        mailSender.send(message);
    }

    @Override
    public void sendConsentRequest(String toEmail, String fullName, String token) {

        // Se crea un objeto que representará el correo electrónico.
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("FaceLit — Solicitud de autorización para menor de edad");
        message.setText(
                "Estimado/a " + fullName + ",\n\n" +
                        "Un menor de edad bajo su responsabilidad se ha registrado en FaceLit.\n\n" +
                        "Para autorizar o rechazar el tratamiento de sus datos personales, " +
                        "ingrese el siguiente código de verificación en la aplicación:\n\n" +
                        token + "\n\n" +
                        "Este código expira en 5 minutos.\n" +
                        "Si no reconoce esta solicitud, ignore este mensaje.\n\n" +
                        "FaceLit");
        mailSender.send(message);

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
