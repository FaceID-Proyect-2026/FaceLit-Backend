package com.FaceLit.backend.academic.service.serviceImpl.academic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.FaceLit.backend.academic.dto.request.academic.ChipRequestDTO;
import com.FaceLit.backend.academic.dto.request.academic.InstructorRequestDTO;
import com.FaceLit.backend.academic.dto.request.academic.TransferChipRequestDTO;
import com.FaceLit.backend.academic.dto.request.academic.UserChipRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.CsvUploadResponseDTO;
import com.FaceLit.backend.academic.dto.response.academic.PendingTransferResponseDTO;
import com.FaceLit.backend.academic.dto.response.academic.UserChipResponseDTO;
import com.FaceLit.backend.academic.exception.AcademicException;
import com.FaceLit.backend.academic.model.academic.ChangeHistory;
import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.model.academic.CsvPendingTransfer;
import com.FaceLit.backend.academic.model.academic.Instructor;
import com.FaceLit.backend.academic.model.academic.InstructorProgram;
import com.FaceLit.backend.academic.model.academic.Program;
import com.FaceLit.backend.academic.model.academic.UserChip;
import com.FaceLit.backend.academic.model.enums.AcademicState;
import com.FaceLit.backend.academic.model.enums.ChangeAction;
import com.FaceLit.backend.academic.model.enums.InstructorType;
import com.FaceLit.backend.academic.model.enums.PendingTransferStatus;
import com.FaceLit.backend.academic.repository.ChangeHistoryRepository;
import com.FaceLit.backend.academic.repository.ChipRepository;
import com.FaceLit.backend.academic.repository.CsvPendingTransferRepository;
import com.FaceLit.backend.academic.repository.InstructorProgramRepository;
import com.FaceLit.backend.academic.repository.InstructorRepository;
import com.FaceLit.backend.academic.repository.ProgramRepository;
import com.FaceLit.backend.academic.repository.UserChipRepository;
import com.FaceLit.backend.academic.service.academic.ChipService;
import com.FaceLit.backend.academic.service.academic.CsvAcademicService;
import com.FaceLit.backend.academic.service.academic.InstructorService;
import com.FaceLit.backend.academic.service.academic.UserChipService;
import com.FaceLit.backend.auth.dto.request.roleandpermission.AssignRoleRequestDTO;
import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.auth.model.enums.CredentialStatus;
import com.FaceLit.backend.auth.model.enums.RoleName;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.service.roleandpermission.AdminRoleService;
import com.FaceLit.backend.notification.dto.request.CreateNotificationRequestDTO;
import com.FaceLit.backend.notification.dto.request.MonolithEventRequestDTO;
import com.FaceLit.backend.notification.service.NotificationService;
import com.FaceLit.backend.shared.constants.AppConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
// Facade/orquestador: recibe la carga CSV y coordina los servicios de programa,
// ficha, instructor y aprendiz sin exponer repositorios al controller.
public class CsvAcademicServiceImpl implements CsvAcademicService {

    private static final Logger log = LoggerFactory.getLogger(CsvAcademicServiceImpl.class);

    private record Row(int number, Map<String, String> cells) {
        String value(String key) {
            return cells.getOrDefault(key, "");
        }
    }
    private static final Pattern DOCUMENT = Pattern.compile(
            "\\d{" + AppConstants.DOCUMENT_NUMBER_MIN_LENGTH + "," + AppConstants.DOCUMENT_NUMBER_MAX_LENGTH
                    + "}");
    private static final Pattern PROGRAM_CODE = Pattern.compile("[A-Za-z0-9]{"
            + AppConstants.PROGRAM_CODE_MIN_LENGTH + "," + AppConstants.PROGRAM_CODE_MAX_LENGTH + "}");
    private static final Pattern CHIP_CODE = Pattern.compile("\\d{" + AppConstants.CHIP_CODE_LENGTH + "}");
    private static final Pattern NAME = Pattern.compile("[\\p{L}]+(?: [\\p{L}]+)*");

    private static final SecureRandom RANDOM = new SecureRandom();
    private final ProgramRepository programRepository;
    private final ChipRepository chipRepository;
    private final InstructorRepository instructorRepository;
    private final InstructorProgramRepository instructorProgramRepository;
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final UserChipRepository userChipRepository;
    private final CsvPendingTransferRepository pendingTransferRepository;
    private final ChangeHistoryRepository changeHistoryRepository;
    private final ChipService chipService;
    private final InstructorService instructorService;
    private final UserChipService userChipService;

    private final PasswordEncoder passwordEncoder;
    private final AdminRoleService adminRoleService;
    private final UserRoleRepository userRoleRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public CsvAcademicServiceImpl(
            ProgramRepository programRepository,
            ChipRepository chipRepository,
            InstructorRepository instructorRepository,
            InstructorProgramRepository instructorProgramRepository,
            UserRepository userRepository,
            CredentialRepository credentialRepository,
            UserChipRepository userChipRepository,
            CsvPendingTransferRepository pendingTransferRepository,
            ChangeHistoryRepository changeHistoryRepository,
            ChipService chipService,
            InstructorService instructorService,
            UserChipService userChipService,
            PasswordEncoder passwordEncoder,
            AdminRoleService adminRoleService,
            UserRoleRepository userRoleRepository,
            NotificationService notificationService,
            ObjectMapper objectMapper) {
        this.programRepository = programRepository;
        this.chipRepository = chipRepository;
        this.instructorRepository = instructorRepository;
        this.instructorProgramRepository = instructorProgramRepository;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.userChipRepository = userChipRepository;
        this.pendingTransferRepository = pendingTransferRepository;
        this.changeHistoryRepository = changeHistoryRepository;
        this.chipService = chipService;
        this.instructorService = instructorService;
        this.userChipService = userChipService;
        this.passwordEncoder = passwordEncoder;
        this.adminRoleService = adminRoleService;
        this.userRoleRepository = userRoleRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] template() {
        return (AppConstants.CSV_TEMPLATE_HEADER + "\n"
                + "programa,,,,,ADSO,,\n"
                + "ficha,,,,,ADSO,2825551,\n"
                + "aprendiz,100234,Juan,Perez,juan.perez@correo.com,,2825551,\n"
                + "instructor,102938475610123,Laura,Gomez,laura.gomez@correo.com,ADSO,,especifico\n"
                + "instructor,105060708012345,Carlos,Ruiz,carlos.ruiz@correo.com,,,transversal\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public CsvUploadResponseDTO upload(MultipartFile file) {
        List<Row> rows = readAndValidate(file);
        CsvUploadResponseDTO result = new CsvUploadResponseDTO();
        Map<String, Program> programs = new HashMap<>();
        Map<String, Chip> chips = new HashMap<>();

        rows.stream().filter(row -> row.value("tipo").equals("programa"))
                .forEach(row -> processProgram(row, programs, result));
        rows.stream().filter(row -> row.value("tipo").equals("ficha"))
                .forEach(row -> processChip(row, programs, chips, result));
        processInstructors(rows, programs, result);
        rows.stream().filter(row -> row.value("tipo").equals("aprendiz"))
                .forEach(row -> processApprentice(row, chips, result));
        notifyCsvUpload(result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingTransferResponseDTO> pendingTransfers() {
        return pendingTransferRepository.findByStatusOrderByCreatedAtDesc(PendingTransferStatus.PENDING).stream()
                .map(PendingTransferResponseDTO::new).toList();
    }

    @Override
    @Transactional
    public UserChipResponseDTO acceptPendingTransfer(UUID idPendingTransfer) {
        CsvPendingTransfer pending = getPending(idPendingTransfer);
        TransferChipRequestDTO dto = new TransferChipRequestDTO();
        dto.setIdNewChip(pending.getProposedChip().getIdChip());
        UserChipResponseDTO response = userChipService.transferChip(pending.getUser().getIdUser(), dto);
        pending.setStatus(PendingTransferStatus.ACCEPTED);
        pending.setResolvedAt(OffsetDateTime.now());
        pendingTransferRepository.save(pending);
        recordCsvHistory(pending, ChangeAction.CSV_CONFIRM);
        notifyTransferApplied(pending);
        return response;
    }

    @Override
    @Transactional
    public void cancelPendingTransfer(UUID idPendingTransfer) {
        CsvPendingTransfer pending = getPending(idPendingTransfer);
        pending.setStatus(PendingTransferStatus.CANCELLED);
        pending.setResolvedAt(OffsetDateTime.now());
        pendingTransferRepository.save(pending);
        recordCsvHistory(pending, ChangeAction.CSV_CANCEL);
        notifyTransferRejected(pending);
    }

    private void processProgram(Row row, Map<String, Program> programs, CsvUploadResponseDTO result) {
        String code = row.value("programa_codigo").toUpperCase(Locale.ROOT);
        String name = row.value("nombre");
        if (code.isBlank()) {
            result.getErroresDeReferencia().add(new CsvUploadResponseDTO.CsvRowError(row.number, row.value("tipo"),
                    "El código de programa es obligatorio en la fila " + row.number + ".", null, null));
            return;
        }
        if (!PROGRAM_CODE.matcher(code).matches()) {
            result.getErroresDeReferencia().add(error(row, "El código de programa de la fila " + row.number
                    + " solo puede contener letras y números, sin espacios."));
            return;
        }
        // La plantilla oficial identifica el programa solo con programa_codigo.
        if (name.isBlank()) {
            name = code;
        }
        if (!NAME.matcher(name).matches() || name.length() < 2 || name.length() > 100) {
            result.getErroresDeReferencia()
                    .add(error(row, "El nombre del programa de la fila " + row.number + " no es válido."));
            return;
        }
        Program program = programRepository.findByProgramCodeIgnoreCase(code).orElse(null);
        if (program == null) {
            program = new Program();
            program.setProgramCode(code);
            program.setState(AcademicState.ACTIVE);
            program.setProgramName(name);
            program = programRepository.save(program);
            result.getCreados().add(new CsvUploadResponseDTO.CsvRowResult(row.number, "programa", code + " creado"));
        } else {
            program.setProgramName(name);
            program = programRepository.save(program);
            result.getActualizados()
                    .add(new CsvUploadResponseDTO.CsvRowResult(row.number, "programa", code + " actualizado"));
        }
        programs.put(code, program);
    }

    private void processChip(Row row, Map<String, Program> programs, Map<String, Chip> chips,
            CsvUploadResponseDTO result) {
        String code = row.value("ficha_codigo");
        String programCode = row.value("programa_codigo").toUpperCase(Locale.ROOT);
        if (!CHIP_CODE.matcher(code).matches()) {
            result.getErroresDeReferencia().add(new CsvUploadResponseDTO.CsvRowError(row.number, "ficha",
                    "El código de ficha de la fila " + row.number + " debe tener 7 dígitos numéricos.", null, code));
            return;
        }
        Program program = programs.get(programCode);
        if (program == null) {
            program = programRepository.findByProgramCodeIgnoreCase(programCode).orElse(null);
        }
        if (program == null) {
            result.getErroresDeReferencia().add(
                    error(row, "El programa '" + programCode + "' indicado en la fila " + row.number + " no existe."));
            return;
        }
        Chip chip = chipRepository.findByChipCode(code).orElse(null);
        if (chip == null) {
            ChipRequestDTO dto = new ChipRequestDTO();
            dto.setIdProgram(program.getIdProgram());
            dto.setChipCode(code);
            chipService.create(program.getIdProgram(), dto);
            chip = chipRepository.findByChipCode(code).orElseThrow();
            result.getCreados().add(new CsvUploadResponseDTO.CsvRowResult(row.number, "ficha", code + " creada"));
        } else if (!chip.getProgram().getIdProgram().equals(program.getIdProgram())) {
            result.getInconsistenciasBloqueadas()
                    .add(new CsvUploadResponseDTO.CsvRowError(row.number, "ficha",
                            "La ficha " + code
                                    + " ya pertenece a otro programa. No se puede cambiar de programa por este medio.",
                            chip.getProgram().getProgramCode(), programCode));
            return;
        } else {
            result.getActualizados()
                    .add(new CsvUploadResponseDTO.CsvRowResult(row.number, "ficha", code + " actualizada"));
        }
        chips.put(code, chip);
    }

    private void processInstructors(List<Row> rows, Map<String, Program> programs, CsvUploadResponseDTO result) {
        Map<String, List<Row>> grouped = rows.stream().filter(row -> row.value("tipo").equals("instructor"))
                .collect(Collectors.groupingBy(row -> row.value("documento"), HashMap::new, Collectors.toList()));
        grouped.forEach((document, instructorRows) -> {
            Row first = instructorRows.get(0);
            if (!DOCUMENT.matcher(document).matches()) {
                result.getErroresDeReferencia().add(error(first,
                    "El documento de la fila " + first.number + " debe tener entre 6 y 15 dígitos numéricos."));
                return;
            }
            String typeValue = first.value("instructor_tipo").toUpperCase(Locale.ROOT);
            InstructorType type = typeValue.equals("ESPECIFICO") ? InstructorType.ESPECIFICO
                    : typeValue.equals("TRANSVERSAL") ? InstructorType.TRANSVERSAL : null;
            if (type == null) {
                result.getErroresDeReferencia()
                        .add(error(first, "El tipo de instructor de la fila " + first.number + " no es válido."));
                return;
            }
            User user = findOrCreateUser(first, result);
            if (user == null) {
                return;
            }
            assignRole(user, RoleName.INSTRUCTOR);
            Instructor instructor = instructorRepository.findByUser_IdUser(user.getIdUser()).orElse(null);
            if (instructor == null) {
                InstructorRequestDTO dto = new InstructorRequestDTO();
                dto.setIdUser(user.getIdUser());
                dto.setInstructorType(type);
                dto.setProgramIds(instructorRows.stream()
                        .map(row -> programs.get(row.value("programa_codigo").toUpperCase(Locale.ROOT)))
                        .filter(program -> program != null).map(Program::getIdProgram).distinct().toList());
                instructorService.create(dto);
                instructorRepository.flush();
                result.getCreados()
                        .add(new CsvUploadResponseDTO.CsvRowResult(first.number, "instructor", document + " creado"));
                return;
            }
            if (instructor.getInstructorType() != type) {
                result.getInconsistenciasBloqueadas()
                        .add(new CsvUploadResponseDTO.CsvRowError(first.number, "instructor",
                                "El instructor " + document + " ya está registrado como "
                                        + instructor.getInstructorType().name()
                                        + ". Cambiar su tipo requiere confirmación manual.",
                                instructor.getInstructorType().name(), type.name()));
                return;
            }
            Set<UUID> existing = instructorProgramRepository.findByInstructor_IdInstructor(instructor.getIdInstructor()).stream()
                    .map(item -> item.getProgram().getIdProgram()).collect(Collectors.toSet());
            List<String> addedPrograms = new ArrayList<>();
            for (Row row : instructorRows) {
                Program program = programs.get(row.value("programa_codigo").toUpperCase(Locale.ROOT));
                if (program != null
                        && !existing.contains(program.getIdProgram())) {
                    InstructorProgram relation = new InstructorProgram();
                    relation.setInstructor(instructor);
                    relation.setProgram(program);
                    instructorProgramRepository.save(relation);
                    existing.add(program.getIdProgram());
                    addedPrograms.add(program.getProgramName() + " (" + program.getProgramCode() + ")");
                }
            }
            notifyCsvInstructorPrograms(instructor, addedPrograms);
            result.getActualizados()
                    .add(new CsvUploadResponseDTO.CsvRowResult(first.number, "instructor", document + " actualizado"));
        });
    }

    private void processApprentice(Row row, Map<String, Chip> chips, CsvUploadResponseDTO result) {
        String document = row.value("documento");
        Chip chip = chips.get(row.value("ficha_codigo"));
        if (chip == null) {
            chip = chipRepository.findByChipCode(row.value("ficha_codigo")).orElse(null);
        }
        if (!DOCUMENT.matcher(document).matches()) {
                result.getErroresDeReferencia().add(error(row,
                    "El documento de la fila " + row.number + " debe tener entre 6 y 15 dígitos numéricos."));
            return;
        }
        if (chip == null) {
            result.getErroresDeReferencia().add(error(row,
                    "La ficha '" + row.value("ficha_codigo") + "' indicada en la fila " + row.number + " no existe."));
            return;
        }
        User user = userRepository.findByDocumentNumber(document).orElse(null);
        if (user == null) {
            user = findOrCreateUser(row, result);
            if (user == null) {
                return;
            }
            assignRole(user, RoleName.APPRENTICE);
            UserChipRequestDTO assignment = new UserChipRequestDTO();
            assignment.setIdUser(user.getIdUser());
            userChipService.assignInitialChip(chip.getIdChip(), assignment);
            result.getCreados()
                    .add(new CsvUploadResponseDTO.CsvRowResult(row.number, "aprendiz", document + " creado"));
            return;
        }
        if (!canUseExistingUserAsApprentice(user)) {
            result.getErroresDeReferencia().add(error(row,
                    "El documento " + document + " ya pertenece a un usuario con otro rol. No se puede registrar como aprendiz."));
            return;
        }
        assignRole(user, RoleName.APPRENTICE);
        User resolvedUser = user;
        UserChip current = userChipRepository.findByUser_IdUserAndState(resolvedUser.getIdUser(), AcademicState.ACTIVE)
                .orElse(null);
        if (current == null || current.getChip().getIdChip().equals(chip.getIdChip())) {
            result.getActualizados()
                    .add(new CsvUploadResponseDTO.CsvRowResult(row.number, "aprendiz", document + " actualizado"));
            return;
        }
        pendingTransferRepository.findByUser_IdUserAndStatus(user.getIdUser(), PendingTransferStatus.PENDING)
                .ifPresent(previous -> {
                    previous.setStatus(PendingTransferStatus.CANCELLED);
                    previous.setResolvedAt(OffsetDateTime.now());
                    pendingTransferRepository.save(previous);
                });
        CsvPendingTransfer pending = new CsvPendingTransfer();
        pending.setUser(user);
        pending.setCurrentChip(current.getChip());
        pending.setProposedChip(chip);
        pending.setSourceRowNumber(row.number);
        pending.setStatus(PendingTransferStatus.PENDING);
        pending = pendingTransferRepository.save(pending);
        result.getTrasladosPendientes()
                .add(new CsvUploadResponseDTO.CsvPendingTransferResult(pending.getIdPendingTransfer(), row.number,
                        user.getFirstName() + " " + user.getLastName(), current.getChip().getChipCode(),
                        chip.getChipCode()));
    }

    private boolean canUseExistingUserAsApprentice(User user) {
        return userRoleRepository.findByUserId(user.getIdUser())
                .map(userRole -> userRole.getRole().getNameRole() == RoleName.APPRENTICE)
                .orElse(true);
    }
    private User findOrCreateUser(Row row, CsvUploadResponseDTO result) {
        User existing = userRepository.findByDocumentNumber(row.value("documento")).orElse(null);
        if (existing != null) {
            String oldFirstName = existing.getFirstName();
            String oldLastName = existing.getLastName();
            String newEmail = row.value("correo").toLowerCase(Locale.ROOT);
            Credential credential = credentialRepository.findByUser(existing).orElse(null);
            String oldEmail = credential != null ? credential.getEmail() : "";

            existing.setFirstName(row.value("nombre"));
            existing.setLastName(row.value("apellido"));
            existing = userRepository.save(existing);

            if (credential == null) {
                if (credentialRepository.existsByEmailIgnoreCase(newEmail)) {
                    result.getErroresDeReferencia().add(error(row,
                            "El correo " + newEmail + " ya esta registrado en otro usuario. Corrige el correo o usa el documento correcto."));
                    return null;
                }
                String password = generatePassword();
                credential = new Credential();
                credential.setUser(existing);
                credential.setEmail(newEmail);
                credential.setPassword(passwordEncoder.encode(password));
                credential.setCredentialStatus(CredentialStatus.ACTIVE);
                credential.setFailedAttempts(0);
                credentialRepository.save(credential);
                result.getContrasenasGeneradas()
                        .add(new CsvUploadResponseDTO.GeneratedPassword(existing.getDocumentNumber(), password));
            } else if (!oldEmail.equalsIgnoreCase(newEmail)) {
                UUID existingUserId = existing.getIdUser();
                credentialRepository.findByEmailIgnoreCase(newEmail)
                        .filter(item -> !item.getUser().getIdUser().equals(existingUserId))
                        .ifPresent(item -> {
                            throw new AcademicException("El correo " + newEmail
                                    + " ya esta registrado en otro usuario. Corrige el correo o usa el documento correcto.",
                                    HttpStatus.CONFLICT);
                        });
                credential.setEmail(newEmail);
                credentialRepository.save(credential);
            }
            notifyCsvPersonalUpdate(row, existing, oldFirstName, oldLastName, oldEmail, newEmail);
            return existing;
        }
        String email = row.value("correo").toLowerCase(Locale.ROOT);
        if (credentialRepository.existsByEmailIgnoreCase(email)) {
            result.getErroresDeReferencia().add(error(row,
                    "El correo " + email + " ya esta registrado en otro usuario. Corrige el correo o usa el documento correcto."));
            return null;
        }

        User user = new User();
        user.setDocumentNumber(row.value("documento"));
        user.setFirstName(row.value("nombre"));
        user.setLastName(row.value("apellido"));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user = userRepository.save(user);
        String password = generatePassword();
        Credential credential = new Credential();
        credential.setUser(user);
        credential.setEmail(email);
        credential.setPassword(passwordEncoder.encode(password));
        credential.setCredentialStatus(CredentialStatus.ACTIVE);
        credential.setFailedAttempts(0);
        credentialRepository.save(credential);
        result.getContrasenasGeneradas()
                .add(new CsvUploadResponseDTO.GeneratedPassword(user.getDocumentNumber(), password));
        return user;
    }

    private void assignRole(User user, RoleName role) {
        AssignRoleRequestDTO request = new AssignRoleRequestDTO();
        request.setRole(role);
        adminRoleService.assignRole(user.getIdUser(), request);
    }

    private CsvPendingTransfer getPending(UUID id) {
        CsvPendingTransfer pending = pendingTransferRepository.findById(id)
                .orElseThrow(() -> new AcademicException("Traslado pendiente no encontrado.", HttpStatus.NOT_FOUND));
        if (pending.getStatus() != PendingTransferStatus.PENDING) {
            throw new AcademicException("Este traslado pendiente ya fue resuelto anteriormente.", HttpStatus.CONFLICT);
        }
        return pending;
    }

    private void recordCsvHistory(CsvPendingTransfer pending, ChangeAction action) {
        ChangeHistory history = new ChangeHistory();
        history.setEntityName("user_chip");
        history.setEntityId(pending.getUser().getIdUser());
        history.setFieldName("chip");
        history.setOldValue(pending.getCurrentChip().getChipCode());
        history.setNewValue(pending.getProposedChip().getChipCode());
        history.setAction(action);
        changeHistoryRepository.save(history);
    }

    private List<Row> readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AcademicException("El archivo no contiene ninguna fila de datos para procesar.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv")) {
            throw new AcademicException("El archivo debe tener extensión .csv.");
        }
        if (file.getSize() > AppConstants.CSV_MAX_FILE_BYTES) {
            throw new AcademicException("El archivo es demasiado grande. El tamaño máximo permitido es 5 MB.");
        }
        String content;
        try {
            content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(file.getBytes()))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new AcademicException(
                    "El archivo no está en formato UTF-8. Vuelve a guardarlo con esa codificación.");
        } catch (IOException exception) {
            throw new AcademicException("No fue posible leer el archivo CSV.");
        }
        List<String> lines = content.lines().toList();
        if (lines.isEmpty()) {
            throw new AcademicException("El archivo no contiene ninguna fila de datos para procesar.");
        }
        List<String> headers = parseLine(lines.get(0)).stream().map(value -> value.trim().toLowerCase(Locale.ROOT))
                .toList();
        if (!headers.contains(AppConstants.CSV_TYPE_COLUMN)) {
            throw new AcademicException("El archivo debe incluir la columna 'tipo'.");
        }
        List<Row> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).isBlank()) {
                continue;
            }
            if (rows.size() >= AppConstants.CSV_MAX_DATA_ROWS) {
                throw new AcademicException("El archivo supera el máximo de " + AppConstants.CSV_MAX_DATA_ROWS
                        + " filas permitidas por carga. Divide la información en varios archivos.");
            }
            List<String> values = parseLine(lines.get(index));
            Map<String, String> cells = new HashMap<>();
            for (int cell = 0; cell < headers.size(); cell++) {
                cells.put(headers.get(cell), cell < values.size() ? values.get(cell).trim() : "");
            }
            String type = cells.getOrDefault("tipo", "").toLowerCase(Locale.ROOT);
            if (!Set.of("programa", "ficha", "aprendiz", "instructor").contains(type)) {
                throw new AcademicException(
                        "Tipo de fila no reconocido en la fila " + (index + 1) + ": '" + type + "'.");
            }
            cells.put("tipo", type);
            rows.add(new Row(index + 1, cells));
        }
        if (rows.isEmpty()) {
            throw new AcademicException("El archivo no contiene ninguna fila de datos para procesar.");
        }
        return rows;
    }

    private List<String> parseLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                quoted = !quoted;
            } else if (current == ',' && !quoted) {
                result.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        result.add(value.toString());
        return result;
    }

    private CsvUploadResponseDTO.CsvRowError error(Row row, String message) {
        return new CsvUploadResponseDTO.CsvRowError(row.number, row.value("tipo"), message, null, null);
    }

    private String generatePassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@$!%*?";
        StringBuilder password = new StringBuilder();
        password.append((char) ('A' + RANDOM.nextInt(26)));
        password.append(RANDOM.nextInt(10));
        password.append("@$!%*?".charAt(RANDOM.nextInt(6)));
        while (password.length() < 10) {
            password.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return password.toString();
    }

    private void notifyCsvUpload(CsvUploadResponseDTO result) {
        try {
            Map<String, Integer> summary = Map.of(
                    "created", result.getCreados().size(),
                    "updated", result.getActualizados().size(),
                    "blocked", result.getInconsistenciasBloqueadas().size(),
                    "errors", result.getErroresDeReferencia().size());
            notificationService.createCoordinatorEvent(new MonolithEventRequestDTO(
                    "csv_upload_done",
                    "Carga de CSV finalizada",
                    "Carga finalizada: " + summary.get("created") + " creados, "
                            + summary.get("updated") + " actualizados, "
                            + summary.get("blocked") + " pendientes de confirmacion, "
                            + summary.get("errors") + " con error.",
                    null,
                    null,
                    "csv_upload",
                    null,
                    json(Map.of("csvSummary", summary))));

            result.getInconsistenciasBloqueadas().forEach(item ->
                    notificationService.createCoordinatorEvent(new MonolithEventRequestDTO(
                            "csv_inconsistency",
                            "Inconsistencia pendiente de revision",
                            item.mensaje(),
                            null,
                            null,
                            item.tipo(),
                            null,
                            json(Map.of(
                                    "conflictRecordType", item.tipo(),
                                    "fromValue", valueOrEmpty(item.valorActual()),
                                    "toValue", valueOrEmpty(item.valorArchivo()))))));

            result.getErroresDeReferencia().forEach(item ->
                    notificationService.createCoordinatorEvent(new MonolithEventRequestDTO(
                            "csv_ref_error",
                            "Fila con error de referencia",
                            item.mensaje(),
                            null,
                            null,
                            item.tipo(),
                            null,
                            json(Map.of("row", item.fila(), "entityType", item.tipo())))));
        } catch (Exception exception) {
            log.warn("No se pudieron generar notificaciones de carga CSV", exception);
        }
    }

    private void notifyTransferApplied(CsvPendingTransfer pending) {
        notifyTransferDecision(
                "csv_transfer_applied",
                "Cambio de ficha aplicado",
                "El aprendiz " + fullName(pending.getUser()) + " fue movido de la ficha "
                        + pending.getCurrentChip().getChipCode() + " a la ficha "
                        + pending.getProposedChip().getChipCode() + ".",
                pending);
    }

    private void notifyTransferRejected(CsvPendingTransfer pending) {
        notifyTransferDecision(
                "csv_transfer_rejected",
                "Cambio de ficha rechazado",
                "Se decidio no aplicar el cambio de ficha del aprendiz " + fullName(pending.getUser())
                        + ". El registro no fue modificado.",
                pending);
    }

    private void notifyTransferDecision(String type, String title, String message, CsvPendingTransfer pending) {
        try {
            notificationService.createCoordinatorEvent(new MonolithEventRequestDTO(
                    type,
                    title,
                    message,
                    null,
                    pending.getIdPendingTransfer(),
                    "csv_pending_transfer",
                    null,
                    json(Map.of(
                            "learnerName", fullName(pending.getUser()),
                            "learnerDocument", pending.getUser().getDocumentNumber(),
                            "fromFichaNumber", pending.getCurrentChip().getChipCode(),
                            "toFichaNumber", pending.getProposedChip().getChipCode()))));
        } catch (Exception exception) {
            log.warn("No se pudo generar notificacion de traslado CSV {}", pending.getIdPendingTransfer(), exception);
        }
    }

    private void notifyCsvPersonalUpdate(Row row, User user, String oldFirstName, String oldLastName, String oldEmail, String newEmail) {
        List<String> changes = new ArrayList<>();
        addChange(changes, "nombre", oldFirstName, user.getFirstName());
        addChange(changes, "apellido", oldLastName, user.getLastName());
        addChange(changes, "correo", oldEmail, newEmail);
        if (changes.isEmpty()) {
            return;
        }
        String type = row.value("tipo").equals("instructor") ? "instructor_profile_updated" : "apprentice_profile_updated";
        try {
            notificationService.createForRecipient(new CreateNotificationRequestDTO(
                    user.getIdUser(),
                    type,
                    "Tus datos fueron modificados",
                    "Coordinacion actualizo tus datos por carga CSV. " + String.join("; ", changes) + ".",
                    user.getIdUser(),
                    "user",
                    null,
                    json(Map.of(
                            "entityType", row.value("tipo"),
                            "document", user.getDocumentNumber(),
                            "channel", "csv"))));
        } catch (Exception exception) {
            log.warn("No se pudo generar notificacion de actualizacion CSV para {}", user.getIdUser(), exception);
        }
    }

    private void notifyCsvInstructorPrograms(Instructor instructor, List<String> addedPrograms) {
        if (addedPrograms.isEmpty()) {
            return;
        }
        try {
            notificationService.createForRecipient(new CreateNotificationRequestDTO(
                    instructor.getUser().getIdUser(),
                    "instructor_assignment_updated",
                    "Tu asignacion academica cambio",
                    "Coordinacion agrego programas por carga CSV: " + String.join(", ", addedPrograms) + ".",
                    instructor.getIdInstructor(),
                    "instructor",
                    null,
                    json(Map.of(
                            "entityType", "instructor",
                            "entityId", instructor.getIdInstructor(),
                            "channel", "csv",
                            "addedPrograms", addedPrograms))));
        } catch (Exception exception) {
            log.warn("No se pudo generar notificacion de programas CSV para {}", instructor.getIdInstructor(), exception);
        }
    }

    private void addChange(List<String> changes, String field, String oldValue, String newValue) {
        String oldSafe = oldValue == null ? "" : oldValue;
        String newSafe = newValue == null ? "" : newValue;
        if (!oldSafe.equals(newSafe)) {
            changes.add(field + ": " + emptyAsNone(oldSafe) + " -> " + emptyAsNone(newSafe));
        }
    }

    private String emptyAsNone(String value) {
        return value == null || value.isBlank() ? "sin dato" : value;
    }

    private String json(Map<String, ?> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
