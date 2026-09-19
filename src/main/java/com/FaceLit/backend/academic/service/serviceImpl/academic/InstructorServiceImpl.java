package com.FaceLit.backend.academic.service.serviceImpl.academic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.auth.model.enums.CredentialStatus;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.academic.service.academic.InstructorService;

@Service
public class InstructorServiceImpl implements InstructorService {

    private final InstructorRepository instructorRepository;
    private final InstructorProgramRepository instructorProgramRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final ChangeHistoryRepository changeHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public InstructorServiceImpl(
            InstructorRepository instructorRepository,
            InstructorProgramRepository instructorProgramRepository,
            ProgramRepository programRepository,
            UserRepository userRepository,
            CredentialRepository credentialRepository,
            ChangeHistoryRepository changeHistoryRepository,
            PasswordEncoder passwordEncoder) {
        this.instructorRepository = instructorRepository;
        this.instructorProgramRepository = instructorProgramRepository;
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.changeHistoryRepository = changeHistoryRepository;
        this.passwordEncoder = passwordEncoder;
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
        if (!document.matches("[A-Za-z0-9]{6,30}")) {
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

    private InstructorResponseDTO createInstructorForUser(User user, InstructorType type, List<UUID> requestedProgramIds, String generatedPassword) {
        if (instructorRepository.existsByUser_IdUser(user.getIdUser())) {
            throw new AcademicException("Este usuario ya está registrado como instructor.", HttpStatus.CONFLICT);
        }
        if (type == InstructorType.ESPECIFICO) {
            List<UUID> effectiveProgramIds = requestedProgramIds == null ? List.of() : requestedProgramIds;
            if (effectiveProgramIds.isEmpty()) {
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
            return new InstructorResponseDTO(instructor, instructorProgramRepository.findAll().stream()
                    .filter(ip -> ip.getInstructor().getIdInstructor().equals(instructor.getIdInstructor()))
                    .toList(), generatedPassword);
        }

        Instructor instructor = new Instructor();
        instructor.setUser(user);
        instructor.setInstructorType(type);
        instructor = instructorRepository.saveAndFlush(instructor);
        recordChange(instructor, "instructor", null, "instructor_type", ChangeAction.CREATE, null, type.name());
        return new InstructorResponseDTO(instructor, List.of(), generatedPassword);
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

    @Override
    @Transactional
    public InstructorResponseDTO update(UUID idInstructor, InstructorRequestDTO dto) {
        Instructor instructor = instructorRepository.findById(idInstructor)
                .orElseThrow(() -> new AcademicException("Instructor no encontrado.", HttpStatus.NOT_FOUND));

        InstructorType oldType = instructor.getInstructorType();
        if (oldType == InstructorType.ESPECIFICO && dto.getInstructorType() == InstructorType.TRANSVERSAL) {
            throw new AcademicException("No se puede cambiar un instructor específico a tipo transversal.", HttpStatus.CONFLICT);
        }

        List<UUID> oldProgramIds = instructorProgramRepository.findAll().stream()
                .filter(ip -> ip.getInstructor().getIdInstructor().equals(idInstructor))
                .map(ip -> ip.getProgram().getIdProgram())
                .toList();

        InstructorType newType = dto.getInstructorType();
        instructor.setInstructorType(newType);
        instructorRepository.save(instructor);

        instructorProgramRepository.findAll().stream()
                .filter(ip -> ip.getInstructor().getIdInstructor().equals(idInstructor))
                .forEach(instructorProgramRepository::delete);

        if (newType == InstructorType.ESPECIFICO) {
            List<UUID> ids = dto.getProgramIds() == null ? List.of() : dto.getProgramIds();
            if (ids.isEmpty()) {
                throw new AcademicException("Un instructor específico debe indicar el programa al que pertenece.", HttpStatus.BAD_REQUEST);
            }
            for (UUID idProgram : ids) {
                programRepository.findById(idProgram)
                        .orElseThrow(() -> new AcademicException("El programa indicado no existe.", HttpStatus.NOT_FOUND));
                InstructorProgram ip = new InstructorProgram();
                ip.setInstructor(instructor);
                ip.setProgram(programRepository.getReferenceById(idProgram));
                instructorProgramRepository.save(ip);
            }
        }

        if (!oldType.equals(newType)) {
            recordChange(instructor, "instructor", oldType.name(), newType.name(), ChangeAction.UPDATE, "instructor_type", oldType.name() + " -> " + newType.name());
        }

        List<UUID> newProgramIds = instructorProgramRepository.findAll().stream()
                .filter(ip -> ip.getInstructor().getIdInstructor().equals(idInstructor))
                .map(ip -> ip.getProgram().getIdProgram())
                .toList();

        if (!oldProgramIds.equals(newProgramIds)) {
            recordChange(instructor, "instructor", oldProgramIds.toString(), newProgramIds.toString(), ChangeAction.UPDATE, "instructor_program", oldProgramIds.toString() + " -> " + newProgramIds.toString());
        }

        return new InstructorResponseDTO(instructor, instructorProgramRepository.findAll().stream()
                .filter(ip -> ip.getInstructor().getIdInstructor().equals(idInstructor))
                .toList());
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
                .filter(instructor -> instructor.getUser().getAccountStatus() == AccountStatus.ACTIVE)
                .map(instructor -> new InstructorResponseDTO(instructor, instructorProgramRepository.findAll().stream()
                        .filter(ip -> ip.getInstructor().getIdInstructor().equals(instructor.getIdInstructor()))
                        .toList()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorResponseDTO findById(UUID idInstructor) {
        Instructor instructor = instructorRepository.findById(idInstructor)
                .orElseThrow(() -> new AcademicException("Instructor no encontrado.", HttpStatus.NOT_FOUND));
        return new InstructorResponseDTO(instructor, instructorProgramRepository.findAll().stream()
                .filter(ip -> ip.getInstructor().getIdInstructor().equals(idInstructor))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorResponseDTO findByUser(UUID idUser) {
        Instructor instructor = instructorRepository.findAll().stream()
                .filter(i -> i.getUser().getIdUser().equals(idUser))
                .findFirst()
                .orElseThrow(() -> new AcademicException("Instructor no encontrado.", HttpStatus.NOT_FOUND));
        return new InstructorResponseDTO(instructor, instructorProgramRepository.findAll().stream()
                .filter(ip -> ip.getInstructor().getIdInstructor().equals(instructor.getIdInstructor()))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorResponseDTO> search(String document, String name, String type) {
        List<Instructor> instructors = instructorRepository.findAll();
        List<InstructorResponseDTO> result = new ArrayList<>();

        for (Instructor instructor : instructors) {
            User user = instructor.getUser();
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                continue;
            }
            String doc = user.getDocumentNumber() == null ? "" : user.getDocumentNumber();
            String fullName = (user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName());
            boolean matchDocument = document == null || document.isBlank() || doc.toLowerCase().contains(document.toLowerCase());
            boolean matchName = name == null || name.isBlank() || fullName.toLowerCase().contains(name.toLowerCase());
            boolean matchType = type == null || type.isBlank() || instructor.getInstructorType().name().equalsIgnoreCase(type);

            if (matchDocument && matchName && matchType) {
                result.add(new InstructorResponseDTO(instructor, instructorProgramRepository.findAll().stream()
                        .filter(ip -> ip.getInstructor().getIdInstructor().equals(instructor.getIdInstructor()))
                        .toList()));
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
                .map(inst -> new InstructorResponseDTO(inst, instructorProgramRepository.findAll().stream()
                        .filter(ip -> ip.getInstructor().getIdInstructor().equals(inst.getIdInstructor()))
                        .toList()))
                .toList();
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
}
