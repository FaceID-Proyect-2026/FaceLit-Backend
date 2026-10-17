package com.FaceLit.backend.auth.service.serviceImpl.security;

import java.time.OffsetDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.dto.request.security.RequestPasswordRecoveryDTO;
import com.FaceLit.backend.auth.dto.request.security.ResetPasswordDTO;
import com.FaceLit.backend.auth.dto.request.security.VerifyTokenDTO;
import com.FaceLit.backend.auth.dto.response.security.PasswordRecoveryResponseDTO;
import com.FaceLit.backend.auth.exception.PasswordRecoveryException;
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.PasswordRecovery;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.PasswordRecoveryRepository;
import com.FaceLit.backend.auth.service.security.PasswordRecoveryService;
import com.FaceLit.backend.shared.constants.AppConstants;
import com.FaceLit.backend.shared.service.EmailService;
import com.FaceLit.backend.shared.util.VerificationCodeGenerator;

import jakarta.transaction.Transactional;

@Service
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

    private final CredentialRepository credentialRepository;
    private final PasswordRecoveryRepository passwordRecoveryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final com.FaceLit.backend.notification.service.NotificationService notifications;

    public PasswordRecoveryServiceImpl(
            CredentialRepository credentialRepository,
            PasswordRecoveryRepository passwordRecoveryRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            VerificationCodeGenerator verificationCodeGenerator,
            com.FaceLit.backend.notification.service.NotificationService notifications) {
        this.credentialRepository = credentialRepository;
        this.passwordRecoveryRepository = passwordRecoveryRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.verificationCodeGenerator = verificationCodeGenerator;
        this.notifications = notifications;
    }

    @Override
    @Transactional
    public PasswordRecoveryResponseDTO requestRecovery(RequestPasswordRecoveryDTO dto) {

        // 1. Buscar las credenciales por email
        // Si no existe → se rechaza explícitamente (decisión de negocio confirmada)
        Credential credential = credentialRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new PasswordRecoveryException("Correo no registrado"));

        User user = credential.getUser();

        // 2 Validar que la cuenta esté ACTIVA
        if (user.getAccountStatus() !=AccountStatus.ACTIVE) {
            throw new PasswordRecoveryException("La cuenta no se encuentra habilitada para recuperar contraseña"); 
        }

        //  2.1 Invalidar el código anterior si existe — igual que resendCode() en RegisterServiceImpl
        passwordRecoveryRepository.findActiveByUser(user).ifPresent(previous -> {
            previous.setUsed(true);
            passwordRecoveryRepository.save(previous);
        });

        // 3. Generar el código numérico de seis dígitos.
        String code = verificationCodeGenerator.generate();

        // 4. Guardar el nuevo registro de recuperación con expiración de 5 minutos
        PasswordRecovery recovery = new PasswordRecovery();
        recovery.setUser(user);
        recovery.setToken(code);
        recovery.setExpirationDate(OffsetDateTime.now()
            .plusMinutes(AppConstants.PASSWORD_RECOVERY_EXPIRY_MINUTES));
        recovery.setUsed(false);
        passwordRecoveryRepository.save(recovery);

        // 5. Enviar el código al correo del usuario
        emailService.sendRecoveryCode(dto.getEmail(), code);

        // 6. Responder confirmando el envío
        return PasswordRecoveryResponseDTO.codeSent();
    }

    @Override
    @Transactional
    public PasswordRecoveryResponseDTO verifyToken(VerifyTokenDTO dto) {
        PasswordRecovery recovery = passwordRecoveryRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new PasswordRecoveryException("Código incorrecto"));

        if (!recovery.isCurrent()) {
            if (recovery.isUsed()) {
                throw new PasswordRecoveryException("Código incorrecto");
            }
            throw new PasswordRecoveryException("Código vencido");
        }

        return PasswordRecoveryResponseDTO.tokenVerified();
    }

    @Override
    @Transactional
    public PasswordRecoveryResponseDTO resetPassword(ResetPasswordDTO dto) {

        // 1. Buscar el código ingresado
        PasswordRecovery recovery = passwordRecoveryRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new PasswordRecoveryException("Código incorrecto"));

        // 2. Verificar que no esté usado y no haya expirado
        if (!recovery.isCurrent()) {
            // isCurrent() ya devuelve false tanto si used=true como si expiró
            // Diferenciamos el mensaje según la causa exacta
            if (recovery.isUsed()) {
                throw new PasswordRecoveryException("Código incorrecto");
            }
            throw new PasswordRecoveryException("Código vencido");
        }

        // 3. Verificar que las contraseñas coincidan
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new PasswordRecoveryException("Las contraseñas no coinciden");
        }

        // 4. Obtener el usuario y su credencial
        User user = recovery.getUser();
        Credential credential = user.getCredential();

        // 5. Hashear y actualizar la contraseña
        // La contraseña anterior se sobreescribe — deja de ser válida automáticamente
        credential.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        // Reseteamos también los intentos fallidos — el usuario recupera acceso limpio
        credential.setFailedAttempts(0);
        credentialRepository.save(credential);

        // 6. Marcar el código como usado — no puede reutilizarse
        recovery.setUsed(true);
        passwordRecoveryRepository.save(recovery);

        credential.setLockedUntil(null);
        String recipientName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
        var metadata = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        metadata.put("action", "password_recovered"); metadata.put("recipientName", recipientName);
        metadata.put("recipientContext", "account_owner"); metadata.put("occurredAt", OffsetDateTime.now().toString());
        notifications.createForRecipient(new com.FaceLit.backend.notification.dto.request.CreateNotificationRequestDTO(
                user.getIdUser(), "password_changed", "Contraseña restablecida",
                "Destinatario: " + recipientName + ". Tu contraseña fue restablecida mediante recuperación. Fecha: "
                        + OffsetDateTime.now().atZoneSameInstant(java.time.ZoneId.of("America/Bogota")).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        + " (hora de Colombia). Recibes este aviso para confirmar un cambio de seguridad de tu cuenta. Ya puedes ingresar con tu nueva contraseña. Si no realizaste este cambio, contacta al coordinador de inmediato.",
                user.getIdUser(), "user_app", null, metadata.toString()));

        // 7. Responder con éxito
        return PasswordRecoveryResponseDTO.passwordReset();
    }

}
