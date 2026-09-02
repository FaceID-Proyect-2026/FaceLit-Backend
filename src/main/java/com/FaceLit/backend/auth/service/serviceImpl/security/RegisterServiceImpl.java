package com.FaceLit.backend.auth.service.serviceImpl.security;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.Optional;
import java.time.Duration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.dto.response.security.RegistrationStatusResponseDTO;
import com.FaceLit.backend.auth.model.legal.Consent;
import com.FaceLit.backend.auth.repository.legal.ConsentRepository;
import com.FaceLit.backend.auth.dto.request.security.EmailVerificationRequestDTO;
import com.FaceLit.backend.auth.dto.request.security.RegisterRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.EmailVerificationResponseDTO;
import com.FaceLit.backend.auth.dto.response.security.RegisterResponseDTO;
import com.FaceLit.backend.auth.exception.RegisterException;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.DocumentType;
import com.FaceLit.backend.auth.model.security.EmailVerification;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.auth.model.enums.CredentialStatus;
import com.FaceLit.backend.auth.model.enums.RoleName;
import com.FaceLit.backend.auth.model.legal.AcceptanceTerms;
import com.FaceLit.backend.auth.model.roleandpermission.UserRole;
import com.FaceLit.backend.auth.model.roleandpermission.Role;
import com.FaceLit.backend.auth.repository.legal.AcceptanceTermsRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.RoleRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.DocumentTypeRepository;
import com.FaceLit.backend.auth.repository.security.EmailVerificationRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.service.security.RegisterService;
import com.FaceLit.backend.shared.constants.AppConstants;
import com.FaceLit.backend.shared.service.EmailService;
import com.FaceLit.backend.shared.util.AgeUtils;
import com.FaceLit.backend.shared.util.VerificationCodeGenerator;

import jakarta.transaction.Transactional;

@Service
public class RegisterServiceImpl implements RegisterService {

    // atributos
    // final = significa que una vez asignada estos atributos en el contructor no
    // puede carmbiar
    private final UserRepository userRepository;
    private final ConsentRepository consentRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final DocumentTypeRepository documentTypeRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AcceptanceTermsRepository acceptanceTermsRepository;
    private final VerificationCodeGenerator verificationCodeGenerator;

    // Inyecion por contructor - es la mejor firma correcta. no por usar @Autowired
    // - (UserRepository9 - Guarda y consulta usuario en BD
    // - (CredentialsRepository) - Guarda y consult credencilaes en BD
    // - (PasswordEncoder) Para hashear la contraseña antes de guardarla
    // - PasswordEncoder, es un objeto en el cual Spring lo incluye ( simpre y cuado
    // - pones "12345" -- guarda "$2a$10$kdjshfksjdhf"
    // lo necesites) y se usa para encriptar contraseñas.

    public RegisterServiceImpl(
            UserRepository userRepository,
            CredentialRepository credentialRepository,
            PasswordEncoder passwordEncoder,
            DocumentTypeRepository documentTypeRepository,
            EmailVerificationRepository emailVerificationRepository,
            EmailService emailService, RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            AcceptanceTermsRepository acceptanceTermsRepository,
            VerificationCodeGenerator verificationCodeGenerator,
            ConsentRepository consentRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.documentTypeRepository = documentTypeRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.emailService = emailService;
        this.roleRepository = roleRepository; // ← nuevo
        this.userRoleRepository = userRoleRepository;
        this.acceptanceTermsRepository = acceptanceTermsRepository;
        this.verificationCodeGenerator = verificationCodeGenerator;
        this.consentRepository = consentRepository;
    }

    @Override
    @Transactional // si algo falla todo se revierte - no queda datos a medias
    public RegisterResponseDTO register(RegisterRequestDTO dto) {

        // 1. Verifica que el documento o este ya registrado
        // throw manda error, osea Si el numero de documento existe, deten el registro y
        // manda error
        if (userRepository.existsByDocumentNumber(dto.getDocumentNumber())) {
            throw new RegisterException("El numero de documento ya esta registrado");

        } // 2. Verifica que el email o este ya registrado
        if (credentialRepository.existsByEmail(dto.getEmail())) {
            throw new RegisterException("El email ya esta registrado");
        }
        // ── NUEVO — 2.1 Validar que haya aceptado los términos ──
        if (!Boolean.TRUE.equals(dto.getAccepted())) {
            throw new RegisterException("No puede continuar sin confirmar lectura o aceptar responsabilidad");
        }

        // // 3. Buscar el tipo de documento que mandó el frontend
        DocumentType documentType = documentTypeRepository.findById(dto.getIdDocumentType())
                .orElseThrow(() -> new RegisterException("Tipo de documento inválido"));

        // 4. Calcula edad
        // Period.between(fechaInicio, fechaFin) lo que hace es calcular la diferencia
        // entre dos fechas
        int age = AgeUtils.calculateAge(dto.getBirthDate());
        String abbreviation = documentType.getAbbreviation(); // mira el tipo de abreviacion seleccionad por el usuario
                                                              // (TI/CC/CE)

        // 5. esto valida que la edad sea coherente con la fecha de nacimiento
        // TI (Tarjeta de identidad) solo es para los menores
        if ("TI".equals(abbreviation) && age >= AppConstants.LEGAL_AGE) {
            throw new RegisterException("La Tarjeta de Identidad es solo para menores de edad");
        }
        if ("CC".equals(abbreviation) && age < AppConstants.LEGAL_AGE) {
            throw new RegisterException("La Cedula de cuidadania solo es para mayores de edad");
        }

        // 6. si es menor de edad debe tener TI
        boolean isMinor = AgeUtils.isMinor(dto.getBirthDate(), documentType.getAbbreviation());

        // 7. Construir y guardar el User
        // Estado inicial PENDING — cambia a ACTIVE cuando verifica el email
        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setDocumentNumber(dto.getDocumentNumber());
        user.setBirthDate(dto.getBirthDate());
        user.setDocumentType(documentType);
        user.setAccountStatus(AccountStatus.PENDING_CONSENT);
        user.setEmailVerified(false);

        // 7.2 Guardar el usuario, antes que la credencial por que la redencial tiene FK
        // de usuario
        // y para crear las credenciales depende del id del usuario
        User savedUser = userRepository.save(user);

        // 8 Contruir y guardar las credencial
        // aqui es donde usamos el passwordEncoder que toma la contraseña del usuario
        // (A123) y la convierte en Hash
        // hash = $2a$10$
        Credential credential = new Credential();
        credential.setEmail(dto.getEmail());
        // Hashear la contraseña - NUNCA guarda en texto plano
        credential.setPassword(passwordEncoder.encode(dto.getPassword()));
        credential.setCredentialStatus(CredentialStatus.ACTIVE);
        credential.setFailedAttempts(0);
        credential.setUser(savedUser); // se
                                       // asocia
                                       // con
                                       // el
                                       // usuario
                                       // guardado
        // 8.1. Guardar la credencial
        credentialRepository.save(credential);

        // // 9. Asignar rol APPRENTICE por defecto
        // Todo usuario empieza como APPRENTICE al registrarse
        // El ADMINISTRATOR puede cambiar el rol después desde el panel de admin
        Role defaultRole = roleRepository.findByNameRole(RoleName.APPRENTICE)
                .orElseThrow(() -> new RegisterException(
                        "Rol por defecto no encontrado. Pide a Mariana que inserte los roles en BD"));

        UserRole userRole = new UserRole();
        userRole.setUser(savedUser);
        userRole.setRole(defaultRole);
        userRole.setAssignmentDate(LocalDate.now());
        userRole.setAssignedAt(OffsetDateTime.now());
        userRoleRepository.save(userRole);

        // ── NUEVO — 9.5 Registrar la aceptación de términos ──
        // Reutilizamos la entidad AcceptanceTerms que ya existe en legal/
        AcceptanceTerms acceptanceTerms = new AcceptanceTerms();
        acceptanceTerms.setUser(savedUser);
        acceptanceTerms.setAccepted(true);
        acceptanceTermsRepository.save(acceptanceTerms);

        // 9.1 Genera el codigo de 6 dijitos aleatorios
        // String.format("%06d", ...) DICE QUE ES DE 6 DIJITOS]
        // EJEMPLO : Si el número es 483 → lo guarda como "000483"
        String code = verificationCodeGenerator.generate();

        // 10. Guarda el codigo en BD con expiracion de 5 minutos
        EmailVerification verification = new EmailVerification();
        verification.setUser(savedUser);
        verification.setCode(code);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(AppConstants.VERIFICATION_EXPIRY_MINUTES));
        emailVerificationRepository.save(verification);

        // 11. Enviar el codigo al correo del usuario
        emailService.sendVerificationCode(dto.getEmail(), code);

        // 12. Responde simpre con PENDING_EMAIL_VERIFICATION
        // El Front sabe que debe mostrar la pantalla de ingreso del código
        return RegisterResponseDTO.pendingEmailVerification(savedUser.getIdUser());

    }

    @Override
    @Transactional
    public EmailVerificationResponseDTO emailVerification(EmailVerificationRequestDTO dto) {

        // 1. buscar el Usuario
        User user = userRepository.findById(dto.getId_user())
                .orElseThrow(() -> new RegisterException("Usuario no encontrado"));

        // 2. Buscar el código de verificación
        EmailVerification verification = emailVerificationRepository.findByUser(user)
                .orElseThrow(() -> new RegisterException("No hay código de verificación activo"));

        // 3. Verificar Vigencia del codigo
        if (!verification.isCurrent()) {
            throw new RegisterException("El código ha expirado. Solicita uno nuevo.");

        }
        // 4. Verifica que el codigo sea correcto
        if (!verification.getCode().equals(dto.getCode())) {
            throw new RegisterException("Código incorrecto");
        }

        // 5. Marca el codigo como usado - no puede reutilizar
        verification.setUsed(true);
        emailVerificationRepository.save(verification);

        // 6. Determinar si es menor o mayor
        String abbreviation = user.getDocumentType().getAbbreviation();
        boolean isMinor = AgeUtils.isMinor(user.getBirthDate(), abbreviation);

        if (isMinor) {
            // EL MENOR VERIFICA SU email pero No se activa todavia
            // Solo se marca el email como verificado - el resto lo hace el Acudiente
            user.setEmailVerified(true);
            user.setAccountStatus(AccountStatus.PENDING_CONSENT); // Debe esperar el acudiente
            userRepository.save(user);

            return EmailVerificationResponseDTO.minorAge();
        }

        // MAYOR DE EDAD - EMAIL VERIFICADO, REGISTRO COMPLETO, PUEDE HACER LOGIN
        // SE GUARDA EN LA BASE DE DATOS
        user.setEmailVerified(true);
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        return EmailVerificationResponseDTO.majorAge();

    }

    @Override
    @Transactional
    public void resendCode(UUID id_user) {

        // 1. Buscar el usuario
        User user = userRepository.findById(id_user).orElseThrow(() -> new RegisterException("Usuario no encontrado"));

        // ── NUEVO: validar cooldown de 60 segundos ──
        Optional<EmailVerification> lastVerification = emailVerificationRepository.findByUser(user);
        if (lastVerification.isPresent()) {
            LocalDateTime lastCreated = lastVerification.get().getCreatedAt();
            long secondsSinceLast = Duration.between(lastCreated, LocalDateTime.now()).getSeconds();
            if (secondsSinceLast < AppConstants.RESEND_COOLDOWN_SECONDS) {
                long remaining = AppConstants.RESEND_COOLDOWN_SECONDS - secondsSinceLast;
                throw new RegisterException("Debes esperar " + remaining + " segundos antes de solicitar otro código");
            }
        }

        // 2. Invalidar el código anterior si existe
        // ifPresent — solo ejecuta si encontró un código previo
        emailVerificationRepository.findByUser(user).ifPresent(v -> {
            v.setUsed(true);
            emailVerificationRepository.save(v);
        });

        // 3. Generar nuevo código de 6 dígitos

        String code = verificationCodeGenerator.generate();

        // 4. Guardar nuevo código con nueva expiración de 5 minutos
        EmailVerification newVerification = new EmailVerification();
        newVerification.setUser(user);
        newVerification.setCode(code);
        newVerification.setExpiresAt(LocalDateTime.now().plusMinutes(AppConstants.VERIFICATION_EXPIRY_MINUTES));
        emailVerificationRepository.save(newVerification);

        // 5. Obtener el email desde la credencial del usuario y enviar
        // user.getCredential() funciona porque la relación está en User con
        // CascadeType.ALL
        String email = user.getCredential().getEmail();
        emailService.sendVerificationCode(email, code);
    }

    // Método nuevo
    @Override
    public RegistrationStatusResponseDTO checkStatus(String documentNumber, String email) {
        User user = null;

        if (documentNumber != null && !documentNumber.isBlank()) {
            user = userRepository.findByDocumentNumber(documentNumber).orElse(null);
        }
        if (user == null && email != null && !email.isBlank()) {
            user = credentialRepository.findByEmail(email).map(Credential::getUser).orElse(null);
        }
        if (user == null) {
            throw new RegisterException("No se encontró un registro con esos datos");
        }

        String abbreviation = user.getDocumentType().getAbbreviation();
        boolean isMinor = AgeUtils.isMinor(user.getBirthDate(), abbreviation);

        // Si falta verificar el email, reenviamos el código automáticamente.
        // Así, retomar el registro siempre exige demostrar acceso real al correo.
        if (!user.isEmailVerified()) {
            try {
                resendCode(user.getIdUser());
            } catch (RegisterException ignored) {
                // Si está en cooldown, el código anterior sigue vigente — no pasa nada
            }
        }

        String consentStatus = null;
        String guardianEmail = null;

        if (isMinor) {
            Optional<Consent> consentOpt = consentRepository.findByUser(user);
            if (consentOpt.isPresent()) {
                consentStatus = consentOpt.get().getConsentStatus().name();
                if (consentOpt.get().getGuardian() != null) {
                    guardianEmail = consentOpt.get().getGuardian().getEmailGuardian();
                }
            }
        }

        return new RegistrationStatusResponseDTO(
                user.getIdUser(),
                user.isEmailVerified(),
                user.getAccountStatus().name(),
                isMinor,
                consentStatus,
                guardianEmail);
    }
}
