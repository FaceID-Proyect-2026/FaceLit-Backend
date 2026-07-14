package com.FaceLit.backend.auth.service.serviceImpl.roleandpermission;

import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.dto.request.roleandpermission.LoginRequestDTO;
import com.FaceLit.backend.auth.dto.response.roleandpermission.LoginResponseDTO;
import

com.FaceLit.backend.auth.exception.LoginException;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.auth.model.roleandpermission.UserRole;
import com.FaceLit.backend.auth.model.roleandpermission.RolePermission;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.RolePermissionRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.service.roleandpermission.JwtService;
import com.FaceLit.backend.auth.service.roleandpermission.LoginService;
import com.FaceLit.backend.auth.service.security.UserSessionService;

import java.util.List;

@Service
public class LoginServiceImpl implements LoginService {

    private final CredentialRepository credentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserSessionService userSessionService;

    public LoginServiceImpl(
            CredentialRepository credentialRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService, UserSessionService userSessionService) {
        this.credentialRepository = credentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userSessionService = userSessionService;
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO dto) {

        // 1. Verificar que aceptó las políticas de privacidad
        // Sin esto el sistema rechaza el acceso — igual que en registro
        if (!Boolean.TRUE.equals(dto.getAceptoPoliticas())) {
            throw new LoginException("Debe aceptar las políticas de privacidad");
        }

        // 2. Buscar las credenciales por email
        // Si no existe ese email en BD → rechazar
        Credential credential = credentialRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new LoginException("Correo electrónico no registrado"));

        // 3. Obtener el usuario asociado a esa credencial
        User user = credential.getUser();

        // 4. Verificar que la cuenta esté ACTIVE
        // PENDING_CONSENT = no verificó email o falta acudiente
        // BLOCKED = demasiados intentos fallidos
        if (!AccountStatus.ACTIVE.equals(user.getAccountStatus())) {
            throw new LoginException("La cuenta no está activa. Verifica tu correo o contacta al administrador");
        }

        // 5. Comparar la contraseña con el hash BCrypt guardado en BD
        // passwordEncoder.matches("1234", "$2a$10$...") → true o false
        if (!passwordEncoder.matches(dto.getPassword(), credential.getPassword())) {

            // Incrementar intentos fallidos
            credential.setFailedAttempts(credential.getFailedAttempts() + 1);
            credentialRepository.save(credential);

            throw new LoginException("Usuario o contraseña incorrectos");
        }

        // 6. Resetear intentos fallidos — login exitoso
        credential.setFailedAttempts(0);
        credentialRepository.save(credential);

        // 7. Buscar el rol del usuario en user_role
        // Si no tiene rol asignado → el administrador no lo ha configurado aún
        UserRole userRole = userRoleRepository.findByUserId(user.getIdUser())
                .orElseThrow(() -> new LoginException("Sin permisos asignados, contacta al administrador"));

        // 8. Obtener el nombre del rol como String
        // Ejemplo: "INSTRUCTOR"
        String roleName = userRole.getRole().getNameRole().name();

        // 9. Buscar todos los permisos asociados a ese rol en role_permission
        // Ejemplo: ["VIEW_OWN_PROFILE", "VIEW_ATTENDANCE", "VIEW_FICHA_ATTENDANCE"]
        List<RolePermission> rolePermissions = rolePermissionRepository
                .findByRole_IdRole(userRole.getRole().getIdRole());

        // 10. Extraer solo los nombres de los permisos como lista de Strings
        List<String> permissions = rolePermissions.stream()
                .map(rp -> rp.getPermission().getNamePermission())
                .collect(Collectors.toList());

        // 11. Generar el JWT con userId, email, rol y permisos
        String token = jwtService.generateToken(user, roleName, permissions);

        // 12. Registrar la sesión — queda guardada en user_session
        // El service encapsula toda la lógica de creación de sesión
        userSessionService.registerSession(user);

        // 12. Devolver la respuesta con el token
        return LoginResponseDTO.success(token, roleName, permissions, user.getIdUser());
    }

}
