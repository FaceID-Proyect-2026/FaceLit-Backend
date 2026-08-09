package com.FaceLit.backend.auth.service.serviceImpl.legal;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.dto.request.legal.ConsentRequestDTO;
import com.FaceLit.backend.auth.dto.request.legal.ConsentVerificationRequestDTO;
import com.FaceLit.backend.auth.dto.response.legal.ConsentResponseDTO;
import com.FaceLit.backend.auth.dto.response.legal.ConsentVerificationResponseDTO;
import com.FaceLit.backend.auth.exception.RegisterException;
import com.FaceLit.backend.auth.model.legal.Consent;
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.auth.model.enums.ConsentStatus;
import com.FaceLit.backend.auth.model.legal.ConsentVerification;
import com.FaceLit.backend.auth.model.legal.Guardian;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.legal.ConsentRepository;
import com.FaceLit.backend.auth.repository.legal.ConsentVerificationRepository;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.legal.GuardianRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.service.legal.ConsentService;
import com.FaceLit.backend.shared.constants.AppConstants;
import com.FaceLit.backend.shared.service.EmailService;
import com.FaceLit.backend.shared.util.VerificationCodeGenerator;

import jakarta.transaction.Transactional;

@Service
public class ConsentServiceImpl implements ConsentService {

    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final ConsentRepository consentRepository;
    private final ConsentVerificationRepository consentVerificationRepository;
    private final CredentialRepository credentialRepository;
    private final EmailService emailService;
    private final VerificationCodeGenerator verificationCodeGenerator;

    public ConsentServiceImpl(
            UserRepository userRepository,
            GuardianRepository guardianRepository,
            ConsentRepository consentRepository,
            ConsentVerificationRepository consentVerificationRepository,
            CredentialRepository credentialRepository,
            EmailService emailService,
            VerificationCodeGenerator verificationCodeGenerator) {

        this.userRepository = userRepository;
        this.guardianRepository = guardianRepository;
        this.consentRepository = consentRepository;
        this.consentVerificationRepository = consentVerificationRepository;
        this.credentialRepository = credentialRepository;
        this.emailService = emailService;
        this.verificationCodeGenerator = verificationCodeGenerator;

    }

    @Override
    @Transactional
    public ConsentResponseDTO requestConsent(ConsentRequestDTO dto) {

        // 1. Buscar el usuario menor
        User user = userRepository.findById(dto.getId_user())
                .orElseThrow(() -> new RegisterException("Usuario no encontrado"));

        // 2. Obtener el email del menor desde su credencial
        String emailMinor = user.getCredential().getEmail();

        // 3. Validar que el correo del acudiente sea diferente al del menor
        if (dto.getEmailGuardian().equalsIgnoreCase(emailMinor)) {
            throw new RegisterException("El correo del acudiente debe ser diferente al del usuario");

        }

        // 4. Crear y guardar el Guardian
        Guardian guardian = new Guardian();
        guardian.setFullName(dto.getFullName());
        guardian.setIdentityDocument(dto.getIdentityDocument());
        guardian.setEmailGuardian(dto.getEmailGuardian());
        Guardian saveGuardian = guardianRepository.save(guardian);

        // 5.Crear y guardar el Consent en estado PENDING
        Consent consent = new Consent();
        consent.setUser(user);
        consent.setGuardian(saveGuardian);
        consent.setConsentStatus(ConsentStatus.PENDING);
        consent.setRequestDate(LocalDateTime.now());
        Consent savedConsent = consentRepository.save(consent);

        // 6. Generar código de 6 dígitos (antes: UUID.randomUUID().toString())
        String code = verificationCodeGenerator.generate();

        // 7. Guardar el token con expiración de 5 minutos
        ConsentVerification verification = new ConsentVerification();
        verification.setConsent(savedConsent);
        verification.setToken(code);
        verification.setExpirationDate(LocalDateTime.now().plusMinutes(AppConstants.VERIFICATION_EXPIRY_MINUTES));
        consentVerificationRepository.save(verification);

        // 8. Enviar correo al acudiente con el enlace de confirmación
        // El enlace lleva el token para que el acudiente acepte o rechace
        emailService.sendConsentRequest(dto.getEmailGuardian(), dto.getFullName(), code);

        // 9. Responder al frontend — estado pendiente
        return ConsentResponseDTO.waitingGuardian(savedConsent.getIdConsent());

    }

    @Override
    @Transactional
    public ConsentVerificationResponseDTO confirmConsent(ConsentVerificationRequestDTO dto) {
        // 1. Buscar el usuario menor
        User user = userRepository.findById(dto.getId_user())
                .orElseThrow(() -> new RegisterException("Usuario no encontrado"));

        // 2. Buscar el consentimiento del menor
        Consent consent = consentRepository.findByUser(user)
                .orElseThrow(() -> new RegisterException("No existe una solicitud de consentimiento"));

        // 3. Buscar la verificación asociada
        ConsentVerification verification = consentVerificationRepository.findByConsent(consent)
                .orElseThrow(() -> new RegisterException("No hay código de verificación activo"));

        // 4. Verificar vigencia
        if (!verification.isCurrent()) {
            return ConsentVerificationResponseDTO.expired();
        }

        // 5. Verificar que el código sea correcto
        if (!verification.getToken().equals(dto.getCode())) {
            throw new RegisterException("Código incorrecto");
        }

        // 6. Marcar como usado
        verification.setUsed(true);
        consentVerificationRepository.save(verification);

        // 7. Actualizar el consentimiento
        consent.setConsentStatus(ConsentStatus.ACCEPTED);
        consent.setResponseDate(LocalDateTime.now());
        consentRepository.save(consent);

        // 8. Activar la cuenta del menor
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        return ConsentVerificationResponseDTO.accepted();
    }

    @Override
    @Transactional
    public ConsentVerificationResponseDTO refuseConsent(ConsentVerificationRequestDTO dto) {

        User user = userRepository.findById(dto.getId_user())
                .orElseThrow(() -> new RegisterException("Usuario no encontrado"));

        Consent consent = consentRepository.findByUser(user)
                .orElseThrow(() -> new RegisterException("No existe una solicitud de consentimiento"));

        ConsentVerification verification = consentVerificationRepository.findByConsent(consent)
                .orElseThrow(() -> new RegisterException("No hay código de verificación activo"));

        if (!verification.isCurrent()) {
            return ConsentVerificationResponseDTO.expired();
        }

        if (!verification.getToken().equals(dto.getCode())) {
            throw new RegisterException("Código incorrecto");
        }

        verification.setUsed(true);
        consentVerificationRepository.save(verification);

        consent.setConsentStatus(ConsentStatus.REJECTED);
        consent.setResponseDate(LocalDateTime.now());
        consentRepository.save(consent);

        user.setAccountStatus(AccountStatus.INACTIVE);
        userRepository.save(user);

        return ConsentVerificationResponseDTO.refused();
    }

    @Override
    @Transactional
    public ConsentResponseDTO resendConsentRequest(UUID id_user) {

        // 1 Buscar el Usuario (menor)

        User user = userRepository.findById(id_user).orElseThrow(() -> new RegisterException("Usuario no encontrado"));

        // 2. Buscar el Consentimiento asociado al Usuario
        Consent consent = consentRepository.findByUser(user)
                .orElseThrow(() -> new RegisterException("No existe una solicitud de consentimiento"));

        // 3. Validar que siga pendiente el menor no haya respondido aun

        if (consent.getConsentStatus() != ConsentStatus.PENDING) {
            throw new RegisterException("El consentimiento ya fue respondido");

        }

        // 4. Buscar el token anterior
        ConsentVerification verification = consentVerificationRepository.findByConsent(consent)
                .orElseThrow(() -> new RegisterException("No existe verificación asociada"));

        // ── NUEVO: cooldown de 60 segundos ──
        long secondsSinceLast = Duration.between(verification.getCreatedAt(), LocalDateTime.now()).getSeconds();
        if (secondsSinceLast < AppConstants.RESEND_COOLDOWN_SECONDS) {
            long remaining = AppConstants.RESEND_COOLDOWN_SECONDS - secondsSinceLast;
            throw new RegisterException("Debes esperar " + remaining + " segundos antes de solicitar otro código");
        }

        // 4.1 Verificar que el token realmente haya expirado
        if (verification.isCurrent()) {
            throw new RegisterException(
                    "Todavía existe un consentimiento vigente");

        }
        // 5. Invalidar el token anterior
        verification.setUsed(true);
        consentVerificationRepository.save(verification);

        // 6. Crear nuevo token
        String code = verificationCodeGenerator.generate();

        // 7. Crear nuevo registro

        ConsentVerification newVerification = new ConsentVerification();
        newVerification.setConsent(consent);
        newVerification.setToken(code);
        newVerification.setExpirationDate(LocalDateTime.now().plusMinutes(AppConstants.VERIFICATION_EXPIRY_MINUTES));

        consentVerificationRepository.save(newVerification);

        // 8. Obtener correo del acudiente

        Guardian guardin = consent.getGuardian();

        // 9. Enviar nuevamente el correo
        emailService.sendConsentRequest(guardin.getEmailGuardian(), guardin.getFullName(), code);

        // 10. Responder
        return ConsentResponseDTO.waitingGuardian(consent.getIdConsent());
    }

}
