package com.FaceLit.backend.auth.service.serviceImpl.security;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.dto.request.roleandpermission.AssignRoleRequestDTO;
import com.FaceLit.backend.auth.dto.request.security.UpdateUserRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.UserDetailResponseDTO;
import com.FaceLit.backend.auth.exception.UserManagementException;
import com.FaceLit.backend.academic.model.enums.UserChipStatus;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.EmailVerification;
import com.FaceLit.backend.auth.model.security.PasswordRecovery;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.model.security.UserSession;
import com.FaceLit.backend.auth.repository.legal.AcceptanceTermsRepository;
import com.FaceLit.backend.auth.repository.legal.ConsentRepository;
import com.FaceLit.backend.auth.repository.legal.ConsentVerificationRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.EmailVerificationRepository;
import com.FaceLit.backend.auth.repository.security.PasswordRecoveryRepository;
import com.FaceLit.backend.auth.repository.security.UserConfigurationRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.repository.security.UserSessionRepository;
import com.FaceLit.backend.auth.service.roleandpermission.AdminRoleService;
import com.FaceLit.backend.auth.service.security.UserManagementService;
import com.FaceLit.backend.shared.constants.AppConstants;
import com.FaceLit.backend.academic.repository.academic.UserChipRepository;
import com.FaceLit.backend.academic.model.academic.UserChip;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// Gestion de usuario
@Service
public class UserManagementServiceImpl implements UserManagementService {

        private final UserRepository userRepository;
        private final CredentialRepository credentialRepository;
        private final UserRoleRepository userRoleRepository;
        private final UserSessionRepository userSessionRepository;
        private final UserConfigurationRepository userConfigurationRepository;
        private final EmailVerificationRepository emailVerificationRepository;
        private final UserChipRepository userChipRepository;
        private final AdminRoleService adminRoleService;
        private final ConsentRepository consentRepository;
        private final ConsentVerificationRepository consentVerificationRepository;
        private final AcceptanceTermsRepository acceptanceTermsRepository;
        private final PasswordRecoveryRepository passwordRecoveryRepository;

        public UserManagementServiceImpl(
                        UserRepository userRepository,
                        CredentialRepository credentialRepository,
                        UserRoleRepository userRoleRepository,
                        UserSessionRepository userSessionRepository,
                        UserConfigurationRepository userConfigurationRepository,
                        EmailVerificationRepository emailVerificationRepository,
                        UserChipRepository userChipRepository,
                        AdminRoleService adminRoleService,
                        ConsentRepository consentRepository,
                        ConsentVerificationRepository consentVerificationRepository,
                        AcceptanceTermsRepository acceptanceTermsRepository,
                        PasswordRecoveryRepository passwordRecoveryRepository) {
                this.userRepository = userRepository;
                this.credentialRepository = credentialRepository;
                this.userRoleRepository = userRoleRepository;
                this.userSessionRepository = userSessionRepository;
                this.userConfigurationRepository = userConfigurationRepository;
                this.emailVerificationRepository = emailVerificationRepository;
                this.userChipRepository = userChipRepository;
                this.adminRoleService = adminRoleService;
                this.consentRepository = consentRepository;
                this.consentVerificationRepository = consentVerificationRepository;
                this.acceptanceTermsRepository = acceptanceTermsRepository;
                this.passwordRecoveryRepository = passwordRecoveryRepository;
        }

        // Convierte un User a UserDetailResponseDTO — reutilizado en los 3 métodos de
        // lectura
        private UserDetailResponseDTO toDTO(User user) {

                String email = credentialRepository.findByUser(user)
                                .map(Credential::getEmail)
                                .orElse(null);

                String roleName = userRoleRepository.findByUserId(user.getIdUser())
                                .map(ur -> ur.getRole().getNameRole().name())
                                .orElse("Sin rol");

                boolean hasSession = userSessionRepository.existsByUser_IdUser(user.getIdUser());

                // Ficha activa, si aplica — null si no tiene (el frontend decide el texto)
                Optional<UserChip> activeChip = userChipRepository
                                .findByUser_IdUserAndState(user.getIdUser(), UserChipStatus.ACTIVE);

                String chipName = activeChip.map(uc -> uc.getChip().getChipName()).orElse(null);
                String chipCode = activeChip.map(uc -> uc.getChip().getChipCode()).orElse(null);
                String programName = activeChip
                                .map(uc -> uc.getChip().getProgram().getProgramName())
                                .orElse(null);

                // Calcula si el JWT del usuario sigue vigente — automático, sin tocar manual
                OffsetDateTime cutoff = OffsetDateTime.now()
                                .minusHours(AppConstants.JWT_EXPIRY_HOURS);
                String sessionStatus = userSessionRepository
                                .hasActiveSession(user.getIdUser(), cutoff)
                                                ? "ACTIVE"
                                                : "INACTIVE";

                return new UserDetailResponseDTO(
                                user.getIdUser(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getDocumentNumber(),
                                user.getDocumentType().getName(),
                                user.getBirthDate(),
                                email,
                                roleName,
                                user.getAccountStatus().name(), // ← estado REAL de cuenta, tal como en la BD
                                sessionStatus, // ← estado de sesión, calculado
                                user.getCreatedAt(),
                                chipName,
                                chipCode,
                                programName,
                                hasSession);
        }

        @Override
        public List<UserDetailResponseDTO> getAllUsers() {
                // RF-10.1: solo usuarios que han iniciado sesión al menos una vez
                return userRepository.findAll().stream()
                                .filter(u -> userSessionRepository.existsByUser_IdUser(u.getIdUser()))
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        @Override
        public List<UserDetailResponseDTO> searchUsers(String query) {
                if (query == null || query.isBlank()) {
                        throw new UserManagementException("No se encontraron usuarios con ese criterio");
                }

                List<UserDetailResponseDTO> results = userRepository.searchByFullNameOrEmail(query).stream()
                                .filter(u -> userSessionRepository.existsByUser_IdUser(u.getIdUser()))
                                .map(this::toDTO)
                                .collect(Collectors.toList());

                if (results.isEmpty()) {
                        throw new UserManagementException("No se encontraron usuarios con ese criterio");
                }

                return results;
        }

        @Override
        public UserDetailResponseDTO getUserDetail(UUID userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UserManagementException("Usuario no encontrado"));
                return toDTO(user);
        }

        @Override
        @Transactional
        public UserDetailResponseDTO updateUser(UUID userId, UpdateUserRequestDTO dto) {

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UserManagementException("Usuario no encontrado"));

                user.setFirstName(dto.getFirstName());
                user.setLastName(dto.getLastName());
                user.setAccountStatus(dto.getAccountStatus());
                userRepository.save(user);

                // Reutiliza la lógica ya existente de asignación de rol — no la duplica
                AssignRoleRequestDTO roleDto = new AssignRoleRequestDTO();
                roleDto.setRole(dto.getRole());
                adminRoleService.assignRole(userId, roleDto);

                return toDTO(user);
        }

        @Override
        @Transactional
        public void deleteUser(UUID userId) {

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UserManagementException("Usuario no encontrado"));

                boolean hasActiveChip = userChipRepository
                                .existsByUser_IdUserAndState(userId, UserChipStatus.ACTIVE);
                if (hasActiveChip) {
                        throw new UserManagementException(
                                        "No se puede eliminar porque está vinculado a una ficha activa. Desvincula primero al aprendiz.");
                }

                // NOTA: pendiente validar "asistencias registradas" — el módulo de
                // asistencia (RF-6, reconocimiento facial) aún no existe.

                // 1. consent_verification depende de consent — se borra primero
                consentRepository.findByUser(user).ifPresent(consent -> {
                        consentVerificationRepository.findByConsent(consent)
                                        .ifPresent(consentVerificationRepository::delete);
                        consentRepository.delete(consent);
                        // Guardian NO se borra aquí — puede estar cubriendo a otro hermano.
                        // Ver nota de diseño en el resumen del módulo.
                });

                // 2. terms_acceptance — todo usuario registrado tiene uno
                acceptanceTermsRepository.findByUser(user)
                                .ifPresent(acceptanceTermsRepository::delete);

                // 3. password_recovery — historial completo, no solo el activo
                List<PasswordRecovery> recoveries = passwordRecoveryRepository.findAllByUser_IdUser(userId);
                passwordRecoveryRepository.deleteAll(recoveries);

                // 4. user_session
                List<UserSession> sessions = userSessionRepository.findByUser_IdUser(userId);
                userSessionRepository.deleteAll(sessions);

                // 5. email_verification — historial completo
                List<EmailVerification> verifications = emailVerificationRepository.findAllByUser_IdUser(userId);
                emailVerificationRepository.deleteAll(verifications);

                // 6. user_configuration
                userConfigurationRepository.findByUser_IdUser(userId)
                                .ifPresent(userConfigurationRepository::delete);

                // 7. user_role
                userRoleRepository.findByUserId(userId)
                                .ifPresent(userRoleRepository::delete);

                // 8. credential
                credentialRepository.findByUser(user)
                                .ifPresent(credentialRepository::delete);

                // 9. user_app — al final, cuando ya no queda nada apuntándole
                userRepository.delete(user);
        }

}
