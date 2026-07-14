package com.FaceLit.backend.auth.service.serviceImpl.legal;

import java.time.LocalDateTime;
import java.util.UUID;

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
import com.FaceLit.backend.shared.service.EmailService;

import jakarta.transaction.Transactional;

@Service
public class ConsentServiceImpl implements ConsentService {

    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final ConsentRepository consentRepository;
    private final ConsentVerificationRepository consentVerificationRepository;
    private final CredentialRepository credentialRepository;
    private final EmailService emailService;

    public ConsentServiceImpl(
            UserRepository userRepository,
            GuardianRepository guardianRepository,
            ConsentRepository consentRepository,
            ConsentVerificationRepository consentVerificationRepository,
            CredentialRepository credentialRepository,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.guardianRepository = guardianRepository;
        this.consentRepository = consentRepository;
        this.consentVerificationRepository = consentVerificationRepository;
        this.credentialRepository = credentialRepository;
        this.emailService = emailService;

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

        // 6. Generar token UUID único de un solo uso
        String token = UUID.randomUUID().toString();

        // 7. Guardar el token con expiración de 5 minutos
        ConsentVerification verification = new ConsentVerification();
        verification.setConsent(savedConsent);
        verification.setToken(token);
        verification.setExpirationDate(LocalDateTime.now().plusMinutes(5));
        consentVerificationRepository.save(verification);

        // 8. Enviar correo al acudiente con el enlace de confirmación
        // El enlace lleva el token para que el acudiente acepte o rechace
        emailService.sendConsentRequest(dto.getEmailGuardian(), dto.getFullName(), token);

        // 9. Responder al frontend — estado pendiente
        return ConsentResponseDTO.waitingGuardian(savedConsent.getIdConsent());

    }

    @Override
    @Transactional
    public ConsentVerificationResponseDTO confirmConsent(ConsentVerificationRequestDTO dto) {
        // 1. Buscar el token en BD

        ConsentVerification verification = consentVerificationRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new RegisterException("Token inválido"));

        // 2. Verificar que no esté expirado ni usado
        if (!verification.isCurrent()) {
            return ConsentVerificationResponseDTO.expired();

        }
        // 3. Marcar token como usado
        verification.setUsed(true);
        consentVerificationRepository.save(verification);

        // 4. Actualizar el estado del consentimiento a ACCEPTED
        Consent consent = verification.getConsent();
        consent.setConsentStatus(ConsentStatus.ACCEPTED);
        consent.setResponseDate(LocalDateTime.now());
        consentRepository.save(consent);

        // 5. Activar la cuenta del usuario
        User user = consent.getUser();
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // 5.1 Activar la cuenta del menor (handled elsewhere) - no direct user field
        // update here
        return ConsentVerificationResponseDTO.accepted();

    }

    @Override
    @Transactional
    public ConsentVerificationResponseDTO refuseConsent(ConsentVerificationRequestDTO dto) {

        // 1. Buscar el token en BD
        ConsentVerification verification = consentVerificationRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new RegisterException("Token inválido"));

        // 2. Verificar que no esté expirado ni usado
        if (!verification.isCurrent()) {
            return ConsentVerificationResponseDTO.expired();

        }
        // 3. Marcar token como usado
        verification.setUsed(true);
        consentVerificationRepository.save(verification);

        // 4. Actualizar el estado del consentimiento a REJECTED
        Consent consent = verification.getConsent();
        consent.setConsentStatus(ConsentStatus.REJECTED);
        consent.setResponseDate(LocalDateTime.now());
        consentRepository.save(consent);

        User user = consent.getUser();
        user.setAccountStatus(AccountStatus.INACTIVE);
        userRepository.save(user);

        // 5. El menor queda inactivo — handled elsewhere
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

        // 4.1 Verificar que el token realmente haya expirado
        if (verification.isCurrent()) {
            throw new RegisterException(
                    "Todavía existe un consentimiento vigente");

        }
        // 5. Invalidar el token anterior
        verification.setUsed(true);
        consentVerificationRepository.save(verification);

        // 6. Crear nuevo token
        String token = UUID.randomUUID().toString();

        // 7. Crear nuevo registro

        ConsentVerification newVerification = new ConsentVerification();
        newVerification.setConsent(consent);
        newVerification.setToken(token);
        newVerification.setExpirationDate(LocalDateTime.now().plusMinutes(5));

        consentVerificationRepository.save(newVerification);

        // 8. Obtener correo del acudiente

        Guardian guardin = consent.getGuardian();

        // 9. Enviar nuevamente el correo
        emailService.sendConsentRequest(guardin.getEmailGuardian(), guardin.getFullName(), token);

        // 10. Responder
        return ConsentResponseDTO.waitingGuardian(consent.getIdConsent());
    }

}
