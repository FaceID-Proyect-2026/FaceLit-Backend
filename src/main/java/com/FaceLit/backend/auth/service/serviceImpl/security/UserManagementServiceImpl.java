package com.FaceLit.backend.auth.service.serviceImpl.security;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.model.academic.Instructor;
import com.FaceLit.backend.academic.model.academic.InstructorProgram;
import com.FaceLit.backend.academic.model.academic.UserChip;
import com.FaceLit.backend.academic.model.enums.AcademicState;
import com.FaceLit.backend.academic.repository.ChipRepository;
import com.FaceLit.backend.academic.repository.InstructorProgramRepository;
import com.FaceLit.backend.academic.repository.InstructorRepository;
import com.FaceLit.backend.academic.repository.UserChipRepository;
import com.FaceLit.backend.auth.dto.request.roleandpermission.AssignRoleRequestDTO;
import com.FaceLit.backend.auth.dto.request.security.CreateManagedUserRequestDTO;
import com.FaceLit.backend.auth.dto.request.security.UpdateUserRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.UserDetailResponseDTO;
import com.FaceLit.backend.auth.dto.response.security.UserListProjection;
import com.FaceLit.backend.auth.exception.UserManagementException;
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.auth.model.enums.CredentialStatus;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.PasswordRecovery;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.model.security.UserSession;
import com.FaceLit.backend.auth.repository.legal.AcceptanceTermsRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.PasswordRecoveryRepository;
import com.FaceLit.backend.auth.repository.security.UserConfigurationRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.repository.security.UserSessionRepository;
import com.FaceLit.backend.auth.service.roleandpermission.AdminRoleService;
import com.FaceLit.backend.auth.service.security.UserManagementService;
import com.FaceLit.backend.notification.dto.request.CreateNotificationRequestDTO;
import com.FaceLit.backend.notification.service.NotificationService;
import com.FaceLit.backend.shared.constants.AppConstants;

import jakarta.transaction.Transactional;

@Service
public class UserManagementServiceImpl implements UserManagementService {

        private final UserRepository userRepository;
        private final CredentialRepository credentialRepository;
        private final UserRoleRepository userRoleRepository;
        private final UserSessionRepository userSessionRepository;
        private final UserConfigurationRepository userConfigurationRepository;
        private final UserChipRepository userChipRepository;
        private final InstructorRepository instructorRepository;
        private final InstructorProgramRepository instructorProgramRepository;
        private final ChipRepository chipRepository;
        private final AdminRoleService adminRoleService;
        private final AcceptanceTermsRepository acceptanceTermsRepository;
        private final PasswordRecoveryRepository passwordRecoveryRepository;
        private final PasswordEncoder passwordEncoder;
        private final NotificationService notificationService;

        public UserManagementServiceImpl(
                        UserRepository userRepository,
                        CredentialRepository credentialRepository,
                        UserRoleRepository userRoleRepository,
                        UserSessionRepository userSessionRepository,
                        UserConfigurationRepository userConfigurationRepository,
                        UserChipRepository userChipRepository,
                        InstructorRepository instructorRepository,
                        InstructorProgramRepository instructorProgramRepository,
                        ChipRepository chipRepository,
                        AdminRoleService adminRoleService,
                        AcceptanceTermsRepository acceptanceTermsRepository,
                        PasswordRecoveryRepository passwordRecoveryRepository,
                        PasswordEncoder passwordEncoder,
                        NotificationService notificationService) {
                this.userRepository = userRepository;
                this.credentialRepository = credentialRepository;
                this.userRoleRepository = userRoleRepository;
                this.userSessionRepository = userSessionRepository;
                this.userConfigurationRepository = userConfigurationRepository;
                this.userChipRepository = userChipRepository;
                this.instructorRepository = instructorRepository;
                this.instructorProgramRepository = instructorProgramRepository;
                this.chipRepository = chipRepository;
                this.adminRoleService = adminRoleService;
                this.acceptanceTermsRepository = acceptanceTermsRepository;
                this.passwordRecoveryRepository = passwordRecoveryRepository;
                this.passwordEncoder = passwordEncoder;
                this.notificationService = notificationService;
        }

        @Override
        public List<UserDetailResponseDTO> getAllUsers() {
                return userRepository.findAllUserSummaries().stream()
                                .map(this::toListDTO)
                                .collect(Collectors.toList());
        }

        @Override
        public long countUsers() {
                return userRepository.count();
        }

        @Override
        public Map<String, Long> getUserCounts() {
                return Map.of(
                                "total", userRepository.count(),
                                "active", userRepository.countByAccountStatus(AccountStatus.ACTIVE),
                                "inactive", userRepository.countByAccountStatus(AccountStatus.INACTIVE),
                                "blocked", userRepository.countByAccountStatus(AccountStatus.BLOCKED));
        }

        @Override
        public List<UserDetailResponseDTO> searchUsers(String query) {
                if (query == null || query.isBlank()) {
                        throw new UserManagementException("No se encontraron usuarios con ese criterio");
                }

                List<UserDetailResponseDTO> results = userRepository.searchUserSummaries(query.trim()).stream()
                                .map(this::toListDTO)
                                .collect(Collectors.toList());

                if (results.isEmpty()) {
                        throw new UserManagementException("No se encontraron usuarios con ese criterio");
                }

                return results;
        }

        private UserDetailResponseDTO toListDTO(UserListProjection user) {
                return new UserDetailResponseDTO(
                                user.getUserId(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getDocumentNumber(),
                                user.getEmail(),
                                user.getRole() != null ? user.getRole().name() : "Sin rol",
                                user.getAccountStatus() != null ? user.getAccountStatus().name() : AccountStatus.INACTIVE.name(),
                                "INACTIVE",
                                user.getRegistrationDate(),
                                null,
                                false,
                                null,
                                null,
                                List.of(),
                                List.of());
        }

        @Override
        public UserDetailResponseDTO getUserDetail(UUID userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UserManagementException("Usuario no encontrado"));
                return toDTO(user);
        }

        @Override
        @Transactional
        public UserDetailResponseDTO createUser(CreateManagedUserRequestDTO dto) {
                if (dto.getRole() != com.FaceLit.backend.auth.model.enums.RoleName.COORDINATOR) {
                        throw new UserManagementException("La gestion administrativa solo puede crear coordinadores");
                }

                String documentNumber = dto.getNumberDocument().trim();
                String email = dto.getEmail().trim().toLowerCase();

                if (userRepository.existsByDocumentNumber(documentNumber)) {
                        throw new UserManagementException("Ya existe un usuario con ese documento");
                }
                if (credentialRepository.existsByEmailIgnoreCase(email)) {
                        throw new UserManagementException("Ya existe un usuario con ese correo");
                }

                User user = new User();
                user.setDocumentNumber(documentNumber);
                user.setFirstName(dto.getFirstName().trim());
                user.setLastName(dto.getLastName().trim());
                user.setAccountStatus(AccountStatus.ACTIVE);
                user = userRepository.save(user);

                Credential credential = new Credential();
                credential.setEmail(email);
                credential.setPassword(passwordEncoder.encode(dto.getPassword()));
                credential.setCredentialStatus(CredentialStatus.ACTIVE);
                credential.setFailedAttempts(0);
                credential.setUser(user);
                credentialRepository.save(credential);

                AssignRoleRequestDTO roleDto = new AssignRoleRequestDTO();
                roleDto.setRole(dto.getRole());
                adminRoleService.assignRole(user.getIdUser(), roleDto);
                notifyUser(user, "user_account_created", "Tu usuario fue creado",
                                "Coordinacion creo tu usuario con rol " + dto.getRole().name() + ".",
                                "{\"entityType\":\"user\",\"entityId\":\"" + user.getIdUser() + "\",\"action\":\"created\"}");

                return new UserDetailResponseDTO(
                                user.getIdUser(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getDocumentNumber(),
                                email,
                                dto.getRole().name(),
                                user.getAccountStatus().name(),
                                "INACTIVE",
                                user.getCreatedAt(),
                                null,
                                false,
                                null,
                                null,
                                List.of(),
                                List.of());
        }

        @Override
        @Transactional
        public UserDetailResponseDTO updateUser(UUID userId, UpdateUserRequestDTO dto) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UserManagementException("Usuario no encontrado"));
                Credential credential = credentialRepository.findByUser(user)
                                .orElseThrow(() -> new UserManagementException("El usuario no tiene credenciales"));

                String documentNumber = dto.getNumberDocument().trim();
                String email = dto.getEmail().trim().toLowerCase();
                String oldDocument = user.getDocumentNumber();
                String oldFirstName = user.getFirstName();
                String oldLastName = user.getLastName();
                String oldEmail = credential.getEmail();
                AccountStatus oldStatus = user.getAccountStatus();
                String oldRole = userRoleRepository.findByUserId(userId)
                                .map(userRole -> userRole.getRole().getNameRole().name())
                                .orElse("Sin rol");

                userRepository.findByDocumentNumber(documentNumber)
                                .filter(existing -> !existing.getIdUser().equals(userId))
                                .ifPresent(existing -> {
                                        throw new UserManagementException("Ya existe un usuario con ese documento");
                                });

                credentialRepository.findByEmailIgnoreCase(email)
                                .filter(existing -> !existing.getUser().getIdUser().equals(userId))
                                .ifPresent(existing -> {
                                        throw new UserManagementException("Ya existe un usuario con ese correo");
                                });

                user.setDocumentNumber(documentNumber);
                user.setFirstName(dto.getFirstName().trim());
                user.setLastName(dto.getLastName().trim());
                user.setAccountStatus(dto.getAccountStatus());
                userRepository.save(user);

                if (oldStatus != dto.getAccountStatus()) {
                        if (dto.getAccountStatus() == AccountStatus.INACTIVE) {
                                credential.setCredentialStatus(CredentialStatus.INACTIVE);
                        } else if (dto.getAccountStatus() == AccountStatus.ACTIVE
                                        && credential.getCredentialStatus() == CredentialStatus.INACTIVE) {
                                credential.setCredentialStatus(CredentialStatus.ACTIVE);
                        }
                }
                credential.setEmail(email);
                credentialRepository.save(credential);

                AssignRoleRequestDTO roleDto = new AssignRoleRequestDTO();
                roleDto.setRole(dto.getRole());
                adminRoleService.assignRole(userId, roleDto);

                List<String> changes = List.of(
                                change("documento", oldDocument, user.getDocumentNumber()),
                                change("nombre", oldFirstName, user.getFirstName()),
                                change("apellido", oldLastName, user.getLastName()),
                                change("correo", oldEmail, credential.getEmail()),
                                change("estado", oldStatus.name(), user.getAccountStatus().name()),
                                change("rol", oldRole, dto.getRole().name()))
                                .stream()
                                .filter(item -> !item.isBlank())
                                .toList();
                if (!changes.isEmpty()) {
                        notifyUser(user, "user_profile_updated", "Tus datos fueron modificados",
                                        "Coordinacion actualizo tus datos. " + String.join("; ", changes) + ".",
                                        "{\"entityType\":\"user\",\"entityId\":\"" + user.getIdUser() + "\",\"action\":\"updated\"}");
                }

                return toDTO(user);
        }

        @Override
        @Transactional
        public void deactivateUser(UUID userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UserManagementException("Usuario no encontrado"));

                if (user.getAccountStatus() != AccountStatus.INACTIVE) {
                        user.setAccountStatus(AccountStatus.INACTIVE);
                        userRepository.save(user);
                }

                credentialRepository.findByUser(user)
                                .ifPresent(credential -> {
                                        credential.setCredentialStatus(CredentialStatus.INACTIVE);
                                        credentialRepository.save(credential);
                                });
        }

        private UserDetailResponseDTO toDTO(User user) {
                String email = credentialRepository.findByUser(user)
                                .map(Credential::getEmail)
                                .orElse(null);

                String roleName = userRoleRepository.findByUserId(user.getIdUser())
                                .map(ur -> ur.getRole().getNameRole().name())
                                .orElse("Sin rol");

                boolean hasSession = userSessionRepository.existsByUser_IdUser(user.getIdUser());
                OffsetDateTime cutoff = OffsetDateTime.now().minusHours(AppConstants.JWT_EXPIRY_HOURS);
                String sessionStatus = userSessionRepository.hasActiveSession(user.getIdUser(), cutoff)
                                ? "ACTIVE"
                                : "INACTIVE";

                Optional<UserChip> activeChip = userChipRepository.findByUser_IdUserAndState(
                                user.getIdUser(),
                                AcademicState.ACTIVE);

                String chipCode = activeChip.map(UserChip::getChip).map(Chip::getChipCode).orElse(null);
                String programName = activeChip.map(UserChip::getChip)
                                .map(Chip::getProgram)
                                .map(program -> program.getProgramName())
                                .orElse(null);

                List<InstructorProgram> instructorPrograms = instructorRepository.findByUser_IdUser(user.getIdUser())
                                .map(Instructor::getIdInstructor)
                                .map(instructorProgramRepository::findByInstructor_IdInstructor)
                                .orElse(List.of());

                List<String> instructorProgramNames = instructorPrograms.stream()
                                .map(InstructorProgram::getProgram)
                                .filter(Objects::nonNull)
                                .map(program -> program.getProgramName())
                                .distinct()
                                .sorted()
                                .toList();

                List<String> instructorChipCodes = instructorPrograms.stream()
                                .map(InstructorProgram::getProgram)
                                .filter(Objects::nonNull)
                                .flatMap(program -> chipRepository.findByProgram_IdProgram(program.getIdProgram()).stream())
                                .map(Chip::getChipCode)
                                .distinct()
                                .sorted()
                                .toList();

                return new UserDetailResponseDTO(
                                user.getIdUser(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getDocumentNumber(),
                                email,
                                roleName,
                                user.getAccountStatus().name(),
                                sessionStatus,
                                user.getCreatedAt(),
                                getSessionExpiresAt(user),
                                hasSession,
                                chipCode,
                                programName,
                                instructorChipCodes,
                                instructorProgramNames);
        }

        private OffsetDateTime getSessionExpiresAt(User user) {
                return userSessionRepository.findByUser_IdUser(user.getIdUser()).stream()
                                .filter(session -> session.getStartDate() != null)
                                .max(Comparator.comparing(UserSession::getStartDate))
                                .map(session -> session.getStartDate().plusHours(AppConstants.JWT_EXPIRY_HOURS))
                                .orElse(null);
        }

        private String change(String field, String oldValue, String newValue) {
                String oldSafe = oldValue == null ? "" : oldValue;
                String newSafe = newValue == null ? "" : newValue;
                if (oldSafe.equals(newSafe)) {
                        return "";
                }
                return field + ": " + emptyAsNone(oldSafe) + " -> " + emptyAsNone(newSafe);
        }

        private String emptyAsNone(String value) {
                return value == null || value.isBlank() ? "sin dato" : value;
        }

        private void notifyUser(User user, String type, String title, String message, String metadata) {
                try {
                        notificationService.createForRecipient(new CreateNotificationRequestDTO(
                                        user.getIdUser(),
                                        type,
                                        title,
                                        message,
                                        user.getIdUser(),
                                        "user",
                                        null,
                                        metadata));
                } catch (RuntimeException ignored) {
                        // La gestion de usuario no debe fallar por una notificacion informativa.
                }
        }
}
