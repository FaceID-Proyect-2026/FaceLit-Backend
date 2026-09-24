package com.FaceLit.backend.auth.service.serviceImpl.security;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
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
                        PasswordEncoder passwordEncoder) {
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
        }

        @Override
        public List<UserDetailResponseDTO> getAllUsers() {
                return userRepository.findAll().stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        @Override
        public List<UserDetailResponseDTO> searchUsers(String query) {
                if (query == null || query.isBlank()) {
                        throw new UserManagementException("No se encontraron usuarios con ese criterio");
                }

                List<UserDetailResponseDTO> results = userRepository.searchByFullNameOrEmail(query.trim()).stream()
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

                return toDTO(user);
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

                credential.setEmail(email);
                credentialRepository.save(credential);

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

                acceptanceTermsRepository.findByUser(user)
                                .ifPresent(acceptanceTermsRepository::delete);

                List<PasswordRecovery> recoveries = passwordRecoveryRepository.findAllByUser_IdUser(userId);
                passwordRecoveryRepository.deleteAll(recoveries);

                List<UserSession> sessions = userSessionRepository.findByUser_IdUser(userId);
                userSessionRepository.deleteAll(sessions);

                userConfigurationRepository.findByUser_IdUser(userId)
                                .ifPresent(userConfigurationRepository::delete);

                userRoleRepository.findByUserId(userId)
                                .ifPresent(userRoleRepository::delete);

                credentialRepository.findByUser(user)
                                .ifPresent(credentialRepository::delete);

                userRepository.delete(user);
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
}
