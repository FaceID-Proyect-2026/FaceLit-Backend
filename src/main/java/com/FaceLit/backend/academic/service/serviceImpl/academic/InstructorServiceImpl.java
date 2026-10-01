package com.FaceLit.backend.academic.service.serviceImpl.academic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.FaceLit.backend.academic.dto.request.academic.InstructorRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.InstructorResponseDTO;
import com.FaceLit.backend.academic.exception.AcademicException;
import com.FaceLit.backend.academic.model.academic.ChangeHistory;
import com.FaceLit.backend.academic.model.academic.Instructor;
import com.FaceLit.backend.academic.model.academic.InstructorProgram;
import com.FaceLit.backend.academic.model.academic.Program;
import com.FaceLit.backend.academic.model.enums.ChangeAction;
import com.FaceLit.backend.academic.model.enums.InstructorType;
import com.FaceLit.backend.academic.repository.ChangeHistoryRepository;
import com.FaceLit.backend.academic.repository.InstructorProgramRepository;
import com.FaceLit.backend.academic.repository.InstructorRepository;
import com.FaceLit.backend.academic.repository.ProgramRepository;
import com.FaceLit.backend.academic.service.academic.InstructorService;
import com.FaceLit.backend.auth.dto.request.roleandpermission.AssignRoleRequestDTO;
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.auth.model.enums.CredentialStatus;
import com.FaceLit.backend.auth.model.enums.RoleName;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.service.roleandpermission.AdminRoleService;
import com.FaceLit.backend.environment.repository.RecordEnvironmentRepository;
import com.FaceLit.backend.notification.dto.request.CreateNotificationRequestDTO;
import com.FaceLit.backend.notification.dto.request.MonolithEventRequestDTO;
import com.FaceLit.backend.notification.service.NotificationService;

@Service
public class InstructorServiceImpl implements InstructorService {

    private final InstructorRepository instructorRepository;
    private final InstructorProgramRepository instructorProgramRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final ChangeHistoryRepository changeHistoryRepository;
    private final RecordEnvironmentRepository recordEnvironmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminRoleService adminRoleService;
    private final NotificationService notificationService;

    public InstructorServiceImpl(
            InstructorRepository instructorRepository,
            InstructorProgramRepository instructorProgramRepository,
            ProgramRepository programRepository,
            UserRepository userRepository,
            CredentialRepository credentialRepository,
            ChangeHistoryRepository changeHistoryRepository,
            RecordEnvironmentRepository recordEnvironmentRepository,
            PasswordEncoder passwordEncoder,
            AdminRoleService adminRoleService,
            NotificationService notificationService) {
        this.instructorRepository = instructorRepository;
        this.instructorProgramRepository = instructorProgramRepository;
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.changeHistoryRepository = changeHistoryRepository;
        this.recordEnvironmentRepository = recordEnvironmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminRoleService = adminRoleService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public InstructorResponseDTO create(InstructorRequestDTO dto) {
        if (dto.getIdUser() != null) {
            User existingUser = userRepository.findById(dto.getIdUser())
                    .orElseThrow(() -> new AcademicException("Usuario no encontrado.", HttpStatus.NOT_FOUND));
            return createInstructorForUser(existingUser, dto.getInstructorType(), dto.getProgramIds(), null);
        }

        String document = normalizeDocument(dto.getDocumento());
        String email = normalizeEmail(dto.getCorreo());
        if (document.isBlank()) {
            throw new AcademicException("El documento es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        if (email.isBlank()) {
            throw new AcademicException("El correo es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        if (!document.matches("\\d{6,15}")) {
            throw new AcademicException("El documento solo puede contener letras y números, y debe tener entre 6 y 30 caracteres.", HttpStatus.BAD_REQUEST);
        }
        if (userRepository.existsByDocumentNumber(document)) {
            throw new AcademicException("Este número de documento ya está registrado.", HttpStatus.CONFLICT);
        }
        if (credentialRepository.existsByEmailIgnoreCase(email)) {
            throw new AcademicException("Ya existe un usuario con ese correo electrónico.", HttpStatus.CONFLICT);
        }

        User newUser = new User();
        newUser.setDocumentNumber(document);
        newUser.setFirstName(dto.getNombre().trim());
        newUser.setLastName(dto.getApellido().trim());
        newUser.setAccountStatus(AccountStatus.ACTIVE);
        newUser = userRepository.saveAndFlush(newUser);

        String password = generatePassword();
        Credential credential = new Credential();
        credential.setUser(newUser);
        credential.setEmail(email);
        credential.setPassword(passwordEncoder.encode(password));
        credential.setCredentialStatus(CredentialStatus.ACTIVE);
        credential.setFailedAttempts(0);
        credentialRepository.saveAndFlush(credential);

        return createInstructorForUser(newUser, dto.getInstructorType(), dto.getProgramIds(), password);
    }

    @Override
    @Transactional
    public InstructorResponseDTO update(UUID idInstructor, InstructorRequestDTO dto) {
        Instructor instructor = instructorRepository.findById(idInstructor)
                .orElseThrow(() -> new AcademicException("Instructor no encontrado.", HttpStatus.NOT_FOUND));

        User user = instructor.getUser();
        Credential credential = user.getCredential();
        String document = normalizeDocument(dto.getDocumento());
        String email = normalizeEmail(dto.getCorreo());
        if (document.isBlank()) {
            throw new AcademicException("El documento es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        if (email.isBlank()) {
            throw new AcademicException("El correo es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        if (!document.matches("\\d{6,15}")) {
            throw new AcademicException("El documento debe contener solo digitos y tener entre 6 y 15 caracteres.", HttpStatus.BAD_REQUEST);
        }

        userRepository.findByDocumentNumber(document)
                .filter(existing -> !existing.getIdUser().equals(user.getIdUser()))
                .ifPresent(existing -> {
                    throw new AcademicException("Este numero de documento ya esta registrado.", HttpStatus.CONFLICT);
                });

        credentialRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getUser().getIdUser().equals(user.getIdUser()))
                .ifPresent(existing -> {
                    throw new AcademicException("Ya existe un usuario con ese correo electronico.", HttpStatus.CONFLICT);
                });

        String oldDocument = user.getDocumentNumber();
        String oldFirstName = user.getFirstName();
        String oldLastName = user.getLastName();
        String oldEmail = credential != null ? credential.getEmail() : "";
        String oldProfile = oldDocument + " / " + oldFirstName + " " + oldLastName + " / " + oldEmail;
        InstructorType oldType = instructor.getInstructorType();

        List<InstructorProgram> currentPrograms = instructorProgramRepository.findByInstructor_IdInstructor(idInstructor);
        List<UUID> oldProgramIds = currentPrograms.stream()
                .map(ip -> ip.getProgram().getIdProgram())
                .toList();
        List<String> oldProgramNames = currentPrograms.stream()
                .map(ip -> ip.getProgram().getProgramName() + " (" + ip.getProgram().getProgramCode() + ")")
                .toList();

        InstructorType newType = dto.getInstructorType();
        user.setDocumentNumber(document);
        user.setFirstName(dto.getNombre().trim());
        user.setLastName(dto.getApellido().trim());
        if (credential == null) {
            throw new AcademicException("El instructor no tiene credencial asociada.", HttpStatus.CONFLICT);
        }
        credential.setEmail(email);
        userRepository.save(user);
        credentialRepository.save(credential);

        instructor.setInstructorType(newType);
        instructorRepository.save(instructor);

        List<UUID> ids = dto.getProgramIds() == null ? List.of() : dto.getProgramIds().stream().distinct().toList();
        if (newType == InstructorType.ESPECIFICO && ids.isEmpty()) {
            throw new AcademicException("Un instructor especifico debe indicar el programa al que pertenece.", HttpStatus.BAD_REQUEST);
        }

        if (!sameIds(oldProgramIds, ids)) {
            instructorProgramRepository.deleteAll(currentPrograms);
            instructorProgramRepository.flush();
        }

        for (UUID idProgram : ids) {
            programRepository.findById(idProgram)
                    .orElseThrow(() -> new AcademicException("El programa indicado no existe.", HttpStatus.NOT_FOUND));
            if (sameIds(oldProgramIds, ids)
                    && instructorProgramRepository.existsByInstructor_IdInstructorAndProgram_IdProgram(idInstructor, idProgram)) {
                continue;
            }
            InstructorProgram ip = new InstructorProgram();
            ip.setInstructor(instructor);
            ip.setProgram(programRepository.getReferenceById(idProgram));
            instructorProgramRepository.save(ip);
        }

        if (!oldType.equals(newType)) {
            recordChange(instructor, "instructor", oldType.name(), newType.name(), ChangeAction.UPDATE, "instructor_type", oldType.name() + " -> " + newType.name());
            notifyInstructor(instructor, "instructor_profile_updated", "Tus datos fueron modificados",
                    "Coordinacion actualizo tu tipo de instructor: " + oldType.name() + " -> " + newType.name() + ".");
        }

        String newProfile = user.getDocumentNumber() + " / " + user.getFirstName() + " " + user.getLastName()
                + " / " + credential.getEmail();
        if (!oldProfile.equals(newProfile)) {
            recordChange(instructor, "instructor", oldProfile, newProfile, ChangeAction.UPDATE, "instructor_profile", newProfile);
            notifyInstructor(instructor, "instructor_profile_updated", "Tus datos fueron modificados",
                    "Coordinacion actualizo tus datos personales. " + describeChanges(Map.of(
                            "documento", values(oldDocument, user.getDocumentNumber()),
                            "nombre", values(oldFirstName, user.getFirstName()),
                            "apellido", values(oldLastName, user.getLastName()),
                            "correo", values(oldEmail, credential.getEmail()))));
        }

        List<UUID> newProgramIds = ids;

        if (!oldProgramIds.equals(newProgramIds)) {
            List<String> newProgramNames = ids.stream()
                    .map(id -> programRepository.findById(id)
                            .map(program -> program.getProgramName() + " (" + program.getProgramCode() + ")")
                            .orElse(id.toString()))
                    .toList();
            recordChange(instructor, "instructor", oldProgramIds.toString(), newProgramIds.toString(), ChangeAction.UPDATE, "instructor_program", oldProgramIds.toString() + " -> " + newProgramIds.toString());
            notifyInstructor(instructor, "instructor_assignment_updated", "Tu asignacion academica cambio",
                    "Coordinacion actualizo tus programas: " + listOrNone(oldProgramNames)
                            + " -> " + listOrNone(newProgramNames) + ".");
        }

        return toResponse(instructor);
    }

    @Override
    @Transactional
    public InstructorResponseDTO reactivate(UUID idInstructor) {
        Instructor instructor = instructorRepository.findById(idInstructor)
                .orElseThrow(() -> new AcademicException("Instructor no encontrado.", HttpStatus.NOT_FOUND));

        User user = instructor.getUser();
        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            return toResponse(instructor);
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        if (user.getCredential() != null) {
            user.getCredential().setCredentialStatus(CredentialStatus.ACTIVE);
        }
        userRepository.save(user);
        recordChange(instructor, "instructor", AccountStatus.INACTIVE.name(), AccountStatus.ACTIVE.name(), ChangeAction.REACTIVATE, "account_status", AccountStatus.ACTIVE.name());

        return toResponse(instructor);
    }
    @Override
    @Transactional
    public void delete(UUID idInstructor) {
        Instructor instructor = instructorRepository.findById(idInstructor)
                .orElseThrow(() -> new AcademicException("Instructor no encontrado.", HttpStatus.NOT_FOUND));

        User user = instructor.getUser();
        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            user.setAccountStatus(AccountStatus.INACTIVE);
            if (user.getCredential() != null) {
                user.getCredential().setCredentialStatus(CredentialStatus.INACTIVE);
            }
            userRepository.save(user);
            recordChange(instructor, "instructor", AccountStatus.ACTIVE.name(), AccountStatus.INACTIVE.name(), ChangeAction.DEACTIVATE, "account_status", AccountStatus.INACTIVE.name());
            return;
        }

        if (instructorProgramRepository.countByInstructor_IdInstructor(idInstructor) > 0) {
            notifyDeleteBlocked(instructor);
            instructorProgramRepository.deleteAll(instructorProgramRepository.findAll().stream()
                    .filter(ip -> ip.getInstructor().getIdInstructor().equals(idInstructor))
                    .toList());
        }

        recordChange(instructor, "instructor", instructor.getInstructorType().name(), null, ChangeAction.DELETE, "instructor", instructor.getInstructorType().name());
        instructorRepository.delete(instructor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorResponseDTO> findAll() {
        return instructorRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorResponseDTO findById(UUID idInstructor) {
        Instructor instructor = instructorRepository.findById(idInstructor)
                .orElseThrow(() -> new AcademicException("Instructor no encontrado.", HttpStatus.NOT_FOUND));
        return toResponse(instructor);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorResponseDTO findByUser(UUID idUser) {
        Instructor instructor = instructorRepository.findAll().stream()
                .filter(i -> i.getUser().getIdUser().equals(idUser))
                .findFirst()
                .orElseThrow(() -> new AcademicException("Instructor no encontrado.", HttpStatus.NOT_FOUND));
        return toResponse(instructor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorResponseDTO> search(String document, String name, String type) {
        List<Instructor> instructors = instructorRepository.findAll();
        List<InstructorResponseDTO> result = new ArrayList<>();

        for (Instructor instructor : instructors) {
            User user = instructor.getUser();
            String doc = user.getDocumentNumber() == null ? "" : user.getDocumentNumber();
            String fullName = (user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName());
            boolean matchDocument = document == null || document.isBlank() || doc.toLowerCase().contains(document.toLowerCase());
            boolean matchName = name == null || name.isBlank() || fullName.toLowerCase().contains(name.toLowerCase());
            boolean matchType = type == null || type.isBlank() || instructor.getInstructorType().name().equalsIgnoreCase(type);

            if (matchDocument && matchName && matchType) {
                result.add(toResponse(instructor));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorResponseDTO> findEligibleByProgram(UUID idProgram) {
        Program program = programRepository.findById(idProgram)
                .orElseThrow(() -> new AcademicException("El programa indicado no existe.", HttpStatus.NOT_FOUND));

        List<Instructor> instructors = instructorRepository.findAll();
        return instructors.stream()
                .filter(inst -> inst.getUser().getAccountStatus() == AccountStatus.ACTIVE)
                .filter(inst -> inst.getInstructorType() == InstructorType.TRANSVERSAL
                        || instructorProgramRepository.findAll().stream()
                                .anyMatch(ip -> ip.getInstructor().getIdInstructor().equals(inst.getIdInstructor())
                                        && ip.getProgram().getIdProgram().equals(program.getIdProgram())))
                .map(this::toResponse)
                .toList();
    }

    private InstructorResponseDTO createInstructorForUser(User user, InstructorType type, List<UUID> requestedProgramIds, String generatedPassword) {
        AssignRoleRequestDTO roleRequest = new AssignRoleRequestDTO();
        roleRequest.setRole(RoleName.INSTRUCTOR);
        adminRoleService.assignRole(user.getIdUser(), roleRequest);

        if (instructorRepository.existsByUser_IdUser(user.getIdUser())) {
            throw new AcademicException("Este usuario ya está registrado como instructor.", HttpStatus.CONFLICT);
        }
        if (type == InstructorType.ESPECIFICO || (requestedProgramIds != null && !requestedProgramIds.isEmpty())) {
            List<UUID> effectiveProgramIds = requestedProgramIds == null ? List.of() : requestedProgramIds;
            if (type == InstructorType.ESPECIFICO && effectiveProgramIds.isEmpty()) {
                throw new AcademicException("Un instructor específico debe indicar el programa al que pertenece.", HttpStatus.BAD_REQUEST);
            }
            List<UUID> validated = new ArrayList<>();
            for (UUID idProgram : effectiveProgramIds) {
                Program program = programRepository.findById(idProgram)
                        .orElseThrow(() -> new AcademicException("El programa indicado no existe.", HttpStatus.NOT_FOUND));
                if (program != null) {
                    validated.add(idProgram);
                }
            }
            if (validated.stream().distinct().count() != validated.size()) {
                validated = validated.stream().distinct().collect(Collectors.toList());
            }

            Instructor newInstructor = new Instructor();
            newInstructor.setUser(user);
            newInstructor.setInstructorType(type);
            Instructor instructor = instructorRepository.saveAndFlush(newInstructor);

            for (UUID idProgram : validated) {
                Program program = programRepository.getReferenceById(idProgram);
                if (!instructorProgramRepository.existsByInstructor_IdInstructorAndProgram_IdProgram(instructor.getIdInstructor(), idProgram)) {
                    InstructorProgram ip = new InstructorProgram();
                    ip.setInstructor(instructor);
                    ip.setProgram(program);
                    instructorProgramRepository.saveAndFlush(ip);
                }
            }

            recordChange(instructor, "instructor", null, "instructor_type", ChangeAction.CREATE, null, type.name());
            String programIdsString = validated.stream().map(UUID::toString).collect(Collectors.joining(","));
            recordChange(instructor, "instructor", null, "instructor_program", ChangeAction.CREATE, null, programIdsString);
        notifyInstructor(instructor, "user_account_created", "Tu usuario de instructor fue creado",
                "Coordinacion creo tu usuario de instructor. Programas asignados: "
                        + listOrNone(instructorProgramRepository.findByInstructor_IdInstructor(instructor.getIdInstructor()).stream()
                                .map(ip -> ip.getProgram().getProgramName() + " (" + ip.getProgram().getProgramCode() + ")")
                                .toList())
                        + ".");
            return toResponse(instructor, generatedPassword);
        }

        Instructor instructor = new Instructor();
        instructor.setUser(user);
        instructor.setInstructorType(type);
        instructor = instructorRepository.saveAndFlush(instructor);
        recordChange(instructor, "instructor", null, "instructor_type", ChangeAction.CREATE, null, type.name());
        notifyInstructor(instructor, "user_account_created", "Tu usuario de instructor fue creado",
                "Coordinacion creo tu usuario de instructor.");
        return toResponse(instructor, generatedPassword);
    }

    private InstructorResponseDTO toResponse(Instructor instructor) {
        return toResponse(instructor, null);
    }

    private InstructorResponseDTO toResponse(Instructor instructor, String initialPassword) {
        UUID idInstructor = instructor.getIdInstructor();
        List<InstructorProgram> programs = instructorProgramRepository.findAll().stream()
                .filter(ip -> ip.getInstructor().getIdInstructor().equals(idInstructor))
                .toList();
        List<UUID> chipIds = recordEnvironmentRepository.findDistinctChipIdsByInstructor(idInstructor);
        return new InstructorResponseDTO(instructor, programs, initialPassword, chipIds);
    }

    private boolean sameIds(List<UUID> currentIds, List<UUID> newIds) {
        return currentIds.size() == newIds.size() && currentIds.containsAll(newIds) && newIds.containsAll(currentIds);
    }

    private String normalizeDocument(String document) {
        return document == null ? "" : document.trim().replace(" ", "").replace("-", "");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String generatePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@$!%*?";
        StringBuilder password = new StringBuilder();
        password.append((char) ('A' + (int) (Math.random() * 26)));
        password.append((int) (Math.random() * 10));
        password.append("@$!%*?".charAt((int) (Math.random() * 6)));
        while (password.length() < 10) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return password.toString();
    }

    private void recordChange(Instructor instructor, String entityName, String oldValue, String newValue, ChangeAction action, String fieldName, String fieldValue) {
        ChangeHistory history = new ChangeHistory();
        history.setEntityName(entityName);
        history.setEntityId(instructor.getIdInstructor());
        history.setFieldName(fieldName != null ? fieldName : "instructor");
        history.setOldValue(oldValue);
        history.setNewValue(newValue != null ? newValue : fieldValue);
        history.setAction(action);
        changeHistoryRepository.save(history);
    }

    private void notifyInstructor(Instructor instructor, String type, String title, String message) {
        try {
            notificationService.createForRecipient(new CreateNotificationRequestDTO(
                    instructor.getUser().getIdUser(),
                    type,
                    title,
                    message,
                    instructor.getIdInstructor(),
                    "instructor",
                    null,
                    "{\"entityType\":\"instructor\",\"entityId\":\"" + instructor.getIdInstructor() + "\"}"));
        } catch (RuntimeException ignored) {
            // La actualizacion academica no debe fallar por notificaciones.
        }
    }

    private String[] values(String oldValue, String newValue) {
        return new String[] { oldValue == null ? "" : oldValue, newValue == null ? "" : newValue };
    }

    private String describeChanges(Map<String, String[]> changes) {
        Map<String, String[]> changed = new LinkedHashMap<>();
        changes.forEach((field, values) -> {
            if (!values[0].equals(values[1])) {
                changed.put(field, values);
            }
        });
        if (changed.isEmpty()) {
            return "No se detectaron diferencias visibles.";
        }
        return changed.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + emptyAsNone(entry.getValue()[0]) + " -> " + emptyAsNone(entry.getValue()[1]))
                .collect(Collectors.joining("; "));
    }

    private String listOrNone(List<String> values) {
        return values == null || values.isEmpty() ? "sin programas" : String.join(", ", values);
    }

    private String emptyAsNone(String value) {
        return value == null || value.isBlank() ? "sin dato" : value;
    }

    private void notifyDeleteBlocked(Instructor instructor) {
        try {
            notificationService.createCoordinatorEvent(new MonolithEventRequestDTO(
                    "academic_delete_blocked",
                    "Intento de eliminacion bloqueado",
                    "No fue posible eliminar permanentemente al instructor "
                            + instructor.getUser().getFirstName() + " " + instructor.getUser().getLastName()
                            + " porque tiene programas asociados.",
                    null,
                    instructor.getIdInstructor(),
                    "instructor",
                    null,
                    "{\"entityType\":\"instructor\",\"entityId\":\"" + instructor.getIdInstructor() + "\"}"));
        } catch (RuntimeException ignored) {
            // La eliminacion academica no debe fallar por notificaciones.
        }
    }
}
