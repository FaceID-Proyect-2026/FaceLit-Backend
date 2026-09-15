package com.FaceLit.backend.auth.service.serviceImpl.roleandpermission;

import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.FaceLit.backend.auth.dto.request.roleandpermission.LoginRequestDTO;
import com.FaceLit.backend.auth.dto.response.roleandpermission.LoginResponseDTO;
import com.FaceLit.backend.auth.exception.LoginException;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.auth.model.enums.CredentialStatus;
import com.FaceLit.backend.auth.model.roleandpermission.UserRole;
import com.FaceLit.backend.auth.model.roleandpermission.RolePermission;
import com.FaceLit.backend.auth.model.legal.AcceptanceTerms;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.RolePermissionRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.repository.legal.AcceptanceTermsRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.service.roleandpermission.JwtService;
import com.FaceLit.backend.auth.service.roleandpermission.LoginService;
import com.FaceLit.backend.auth.service.security.UserSessionService;

import java.util.List;

@Service
public class LoginServiceImpl implements LoginService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final AcceptanceTermsRepository acceptanceTermsRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserSessionService userSessionService;

    public LoginServiceImpl(
            CredentialRepository credentialRepository,
            UserRepository userRepository,
            AcceptanceTermsRepository acceptanceTermsRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService, UserSessionService userSessionService) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.acceptanceTermsRepository = acceptanceTermsRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userSessionService = userSessionService;
    }

    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto) {

        User user = userRepository.findByDocumentNumber(dto.getDocumentNumber())
                .orElseThrow(() -> new LoginException("Documento o contraseña incorrectos"));
        Credential credential = user.getCredential();
        if (credential == null) {
            throw new LoginException("Documento o contraseña incorrectos");
        }

        if (CredentialStatus.BLOCKED.equals(credential.getCredentialStatus())) {
            if (credential.getLockedUntil() == null || credential.getLockedUntil().isAfter(java.time.OffsetDateTime.now())) {
                throw new LoginException("La cuenta está temporalmente bloqueada. Intenta más tarde o recupera tu contraseña");
            }
            credential.setCredentialStatus(CredentialStatus.ACTIVE);
            credential.setFailedAttempts(0);
            credential.setLockedUntil(null);
        }

        if (!CredentialStatus.ACTIVE.equals(credential.getCredentialStatus())) {
            throw new LoginException("La credencial no está habilitada para iniciar sesión");
        }

        // 3. Verificar que la cuenta esté ACTIVE
        // PENDING_CONSENT = no verificó email o falta acudiente
        // BLOCKED = demasiados intentos fallidos
        if (!AccountStatus.ACTIVE.equals(user.getAccountStatus())) {
            throw new LoginException("La cuenta no está activa. Contacta al Coordinador");
        }

        // 4. Comparar la contraseña con el hash BCrypt guardado en BD
        // passwordEncoder.matches("1234", "$2a$10$...") → true o false
        if (!passwordEncoder.matches(dto.getPassword(), credential.getPassword())) {

            // Incrementar intentos fallidos
            int failedAttempts = credential.getFailedAttempts() == null ? 1 : credential.getFailedAttempts() + 1;
            credential.setFailedAttempts(failedAttempts);
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                credential.setCredentialStatus(CredentialStatus.BLOCKED);
                credential.setLockedUntil(java.time.OffsetDateTime.now().plusMinutes(15));
                credentialRepository.save(credential);
                throw new LoginException("La cuenta está temporalmente bloqueada. Intenta más tarde o recupera tu contraseña");
            }
            credentialRepository.save(credential);

            throw new LoginException("Usuario o contraseña incorrectos");
        }

        // 5. Resetear intentos fallidos — login exitoso
        credential.setFailedAttempts(0);
        credential.setCredentialStatus(CredentialStatus.ACTIVE);
        credential.setLockedUntil(null);
        credentialRepository.save(credential);

        if (!acceptanceTermsRepository.existsByUser(user)) {
            if (!Boolean.TRUE.equals(dto.getAccepted())) {
                throw new LoginException("Políticas no aceptadas");
            }
            AcceptanceTerms acceptanceTerms = new AcceptanceTerms();
            acceptanceTerms.setUser(user);
            acceptanceTerms.setAccepted(true);
            acceptanceTermsRepository.save(acceptanceTerms);
        }

        // 6. Buscar el rol del usuario en user_role
        // Si no tiene rol asignado → el administrador no lo ha configurado aún
        UserRole userRole = userRoleRepository.findByUserId(user.getIdUser())
                .orElseThrow(() -> new LoginException("Sin permisos asignados, contacta al administrador"));

        // 7. Obtener el nombre del rol como String
        // Ejemplo: "INSTRUCTOR"
        String roleName = userRole.getRole().getNameRole().name();

        // 8. Buscar todos los permisos asociados a ese rol en role_permission
        // Ejemplo: ["VIEW_OWN_PROFILE", "VIEW_ATTENDANCE", "VIEW_FICHA_ATTENDANCE"]
        List<RolePermission> rolePermissions = rolePermissionRepository
                .findByRole_IdRole(userRole.getRole().getIdRole());

        // 9. Extraer solo los nombres de los permisos como lista de Strings
        List<String> permissions = rolePermissions.stream()
                .map(rp -> rp.getPermission().getNamePermission())
                .collect(Collectors.toList());

        // 10. Generar el JWT con userId, email, rol y permisos
        String token = jwtService.generateToken(user, roleName, permissions);

        // 11. Registrar la sesión — queda guardada en user_session
        // El service encapsula toda la lógica de creación de sesión
        userSessionService.registerSession(user);

        // 12. Devolver la respuesta con el token
        return LoginResponseDTO.success(token, roleName, permissions, user.getIdUser());
    }

}
