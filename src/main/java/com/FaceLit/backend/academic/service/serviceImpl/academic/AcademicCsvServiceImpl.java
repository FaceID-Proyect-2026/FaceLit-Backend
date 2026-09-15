package com.FaceLit.backend.academic.service.serviceImpl.academic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.FaceLit.backend.academic.dto.response.academic.CsvImportSummaryDTO;
import com.FaceLit.backend.academic.exception.UserChipException;
import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.model.academic.Instructor;
import com.FaceLit.backend.academic.model.academic.InstructorProgram;
import com.FaceLit.backend.academic.model.academic.PendingTransfer;
import com.FaceLit.backend.academic.model.academic.Program;
import com.FaceLit.backend.academic.model.academic.UserChip;
import com.FaceLit.backend.academic.model.enums.ChangeAction;
import com.FaceLit.backend.academic.model.enums.ChipState;
import com.FaceLit.backend.academic.model.enums.InstructorType;
import com.FaceLit.backend.academic.model.enums.PendingTransferStatus;
import com.FaceLit.backend.academic.model.enums.ProgramState;
import com.FaceLit.backend.academic.model.enums.UserChipStatus;
import com.FaceLit.backend.academic.repository.academic.ChipRepository;
import com.FaceLit.backend.academic.repository.academic.InstructorProgramRepository;
import com.FaceLit.backend.academic.repository.academic.InstructorRepository;
import com.FaceLit.backend.academic.repository.academic.PendingTransferRepository;
import com.FaceLit.backend.academic.repository.academic.ProgramRepository;
import com.FaceLit.backend.academic.repository.academic.UserChipRepository;
import com.FaceLit.backend.academic.service.academic.AcademicCsvService;
import com.FaceLit.backend.academic.service.academic.UserChipService;
import com.FaceLit.backend.academic.service.audit.ChangeHistoryService;
import com.FaceLit.backend.auth.model.enums.RoleName;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.service.security.InstitutionalAccountService;

import jakarta.transaction.Transactional;

@Service
public class AcademicCsvServiceImpl implements AcademicCsvService {

    private static final long MAX_BYTES = 50L * 1024 * 1024;
    private static final int MAX_ROWS = 5000;
    private static final Pattern DOCUMENT = Pattern.compile("^[0-9]{10}$");
    private static final Pattern NAME = Pattern.compile("^[A-Za-zÁÉÍÓÚÜáéíóúüÑñ]{2,60}( [A-Za-zÁÉÍÓÚÜáéíóúüÑñ]{2,60})*$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PROGRAM_CODE = Pattern.compile("^[A-Za-z0-9]{2,15}$");
    private static final Pattern CHIP_CODE = Pattern.compile("^[0-9]{7}$");
    private final ProgramRepository programRepository;
    private final ChipRepository chipRepository;
    private final UserRepository userRepository;
    private final UserChipRepository userChipRepository;
    private final InstructorRepository instructorRepository;
    private final InstructorProgramRepository instructorProgramRepository;
    private final PendingTransferRepository pendingTransferRepository;
    private final InstitutionalAccountService accountService;
    private final UserChipService userChipService;
    private final ChangeHistoryService historyService;

    public AcademicCsvServiceImpl(ProgramRepository programRepository, ChipRepository chipRepository,
            UserRepository userRepository, UserChipRepository userChipRepository,
            InstructorRepository instructorRepository, InstructorProgramRepository instructorProgramRepository,
            PendingTransferRepository pendingTransferRepository, InstitutionalAccountService accountService,
            UserChipService userChipService, ChangeHistoryService historyService) {
        this.programRepository = programRepository;
        this.chipRepository = chipRepository;
        this.userRepository = userRepository;
        this.userChipRepository = userChipRepository;
        this.instructorRepository = instructorRepository;
        this.instructorProgramRepository = instructorProgramRepository;
        this.pendingTransferRepository = pendingTransferRepository;
        this.accountService = accountService;
        this.userChipService = userChipService;
        this.historyService = historyService;
    }

    @Override
    public CsvImportSummaryDTO importFile(MultipartFile file) {
        validateFile(file);
        CsvImportSummaryDTO summary = new CsvImportSummaryDTO();
        try (CSVParser parser = CSVParser.parse(file.getInputStream(), StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).build())) {
            Map<String, Integer> headers = parser.getHeaderMap().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(e -> e.getKey().trim().toLowerCase(Locale.ROOT), Map.Entry::getValue));
            if (!headers.containsKey("tipo")) {
                throw new UserChipException("El archivo debe incluir la columna 'tipo'.");
            }
            int rowCount = 0;
            for (CSVRecord record : parser) {
                if (++rowCount > MAX_ROWS) {
                    throw new UserChipException("El archivo supera el máximo de 5000 filas permitidas por carga.");
                }
                processRow(record, headers, rowCount + 1, summary);
            }
            if (rowCount == 0) {
                throw new UserChipException("El archivo no contiene ninguna fila de datos para procesar.");
            }
            return summary;
        } catch (IOException exception) {
            throw new UserChipException("No fue posible leer el archivo CSV.");
        }
    }

    @Override
    @Transactional
    public PendingTransfer confirmTransfer(UUID id) {
        PendingTransfer pending = pendingTransferRepository.findById(id)
                .orElseThrow(() -> new UserChipException("Traslado pendiente no encontrado"));
        if (pending.getStatus() != PendingTransferStatus.PENDING) {
            throw new UserChipException("El traslado ya fue resuelto");
        }
        userChipService.transferChip(pending.getUser().getIdUser(), pending.getProposedChip().getIdChip(),
            ChangeAction.CSV_CONFIRM);
        pending.setStatus(PendingTransferStatus.CONFIRMED);
        return pendingTransferRepository.save(pending);
    }

    @Override
    @Transactional
    public PendingTransfer cancelTransfer(UUID id) {
        PendingTransfer pending = pendingTransferRepository.findById(id)
                .orElseThrow(() -> new UserChipException("Traslado pendiente no encontrado"));
        if (pending.getStatus() != PendingTransferStatus.PENDING) {
            throw new UserChipException("El traslado ya fue resuelto");
        }
        pending.setStatus(PendingTransferStatus.CANCELLED);
        historyService.record("user_chip", pending.getUser().getIdUser(), ChangeAction.CSV_CANCEL, "chip",
                pending.getCurrentChip().getChipCode(), pending.getProposedChip().getChipCode(), null);
        return pendingTransferRepository.save(pending);
    }

    private void processRow(CSVRecord row, Map<String, Integer> headers, int rowNumber, CsvImportSummaryDTO summary) {
        String type = value(row, headers, "tipo").toLowerCase(Locale.ROOT);
        try {
            switch (type) {
                case "programa" -> processProgram(row, headers, summary);
                case "ficha" -> processChip(row, headers, rowNumber, summary);
                case "aprendiz" -> processApprentice(row, headers, rowNumber, summary);
                case "instructor" -> processInstructor(row, headers, summary);
                default -> summary.error("Tipo de fila no reconocido en la fila " + rowNumber + ".");
            }
        } catch (RuntimeException exception) {
            summary.error("Error en la fila " + rowNumber + ": " + exception.getMessage());
        }
    }

    private void processProgram(CSVRecord row, Map<String, Integer> headers, CsvImportSummaryDTO summary) {
        String code = required(row, headers, "programa_codigo");
        validateProgramCode(code);
        String name = value(row, headers, "nombre");
        if (name.isBlank()) {
            name = code;
        } else {
            validateName(name, "nombre");
        }
        Program program = programRepository.findByProgramCodeIgnoreCase(code).orElse(null);
        if (program == null) {
            program = new Program();
            program.setProgramCode(code.toUpperCase(Locale.ROOT));
            program.setProgramName(name);
            program.setState(ProgramState.ACTIVE);
            program = programRepository.save(program);
            summary.created();
            historyService.record("program", program.getIdProgram(), ChangeAction.CSV_LOAD, "program", null,
                    name + " (" + code + ")", null);
        } else {
            program.setProgramName(name);
            programRepository.save(program);
            summary.updated();
            historyService.record("program", program.getIdProgram(), ChangeAction.CSV_LOAD, "program_name", null, name, null);
        }
    }

    private void processChip(CSVRecord row, Map<String, Integer> headers, int rowNumber, CsvImportSummaryDTO summary) {
        String code = required(row, headers, "ficha_codigo");
        String programCode = required(row, headers, "programa_codigo");
        validateChipCode(code);
        validateProgramCode(programCode);
        Program program = programRepository.findByProgramCodeIgnoreCase(programCode)
                .orElseThrow(() -> new UserChipException("El programa '" + programCode + "' no existe."));
        Chip chip = chipRepository.findByChipCode(code).orElse(null);
        if (chip == null) {
            chip = new Chip();
            chip.setChipCode(code);
            chip.setProgram(program);
            chip.setState(ChipState.ACTIVE);
            chip = chipRepository.save(chip);
            summary.created();
            historyService.record("chip", chip.getIdChip(), ChangeAction.CSV_LOAD, "chip", null, code, null);
        } else {
            chip.setProgram(program);
            chipRepository.save(chip);
            summary.updated();
        }
    }

    private void processApprentice(CSVRecord row, Map<String, Integer> headers, int rowNumber, CsvImportSummaryDTO summary) {
        String document = required(row, headers, "documento");
        String firstName = required(row, headers, "nombre");
        String lastName = required(row, headers, "apellido");
        String email = required(row, headers, "correo");
        String chipCode = required(row, headers, "ficha_codigo");
        validatePerson(document, firstName, lastName, email);
        validateChipCode(chipCode);
        Chip chip = chipRepository.findByChipCode(chipCode)
                .orElseThrow(() -> new UserChipException("La ficha '" + chipCode + "' no existe."));
        InstitutionalAccountService.ProvisionedAccount account = accountService.create(
                firstName, lastName, document, email, RoleName.APPRENTICE);
        if (account.created()) {
            summary.temporaryPassword(document, firstName + " " + lastName, "APRENDIZ",
                account.temporaryPassword());
        }
        User user = account.user();
        UserChip active = userChipRepository.findByUser_IdUserAndState(user.getIdUser(), UserChipStatus.ACTIVE).orElse(null);
        if (active == null) {
            UserChip relation = new UserChip();
            relation.setUser(user);
            relation.setChip(chip);
            relation.setAssignmentDate(java.time.OffsetDateTime.now());
            relation.setState(UserChipStatus.ACTIVE);
            relation = userChipRepository.save(relation);
            summary.created();
            historyService.record("user_chip", user.getIdUser(), ChangeAction.CSV_LOAD, "chip", null, chipCode, null);
        } else if (!active.getChip().getIdChip().equals(chip.getIdChip())) {
            PendingTransfer pending = new PendingTransfer();
            pending.setUser(user);
            pending.setCurrentChip(active.getChip());
            pending.setProposedChip(chip);
            pending.setSourceRow(rowNumber);
            pending.setStatus(PendingTransferStatus.PENDING);
            pending = pendingTransferRepository.save(pending);
            summary.pending(pending.getIdPendingTransfer());
        } else {
            summary.updated();
        }
    }

    private void processInstructor(CSVRecord row, Map<String, Integer> headers, CsvImportSummaryDTO summary) {
        String document = required(row, headers, "documento");
        String firstName = required(row, headers, "nombre");
        String lastName = required(row, headers, "apellido");
        String email = required(row, headers, "correo");
        validatePerson(document, firstName, lastName, email);
        String rawType = required(row, headers, "instructor_tipo").toUpperCase(Locale.ROOT);
        InstructorType type;
        try {
            type = InstructorType.valueOf(rawType);
        } catch (IllegalArgumentException exception) {
            throw new UserChipException("El tipo de instructor no es válido.");
        }
        String programCode = value(row, headers, "programa_codigo");
        if (!programCode.isBlank()) validateProgramCode(programCode);
        if (type == InstructorType.ESPECIFICO && programCode.isBlank()) {
            throw new UserChipException("Un instructor específico debe indicar el programa al que pertenece.");
        }
        InstitutionalAccountService.ProvisionedAccount account = accountService.create(
                firstName, lastName, document, email, RoleName.INSTRUCTOR);
        if (account.created()) {
            summary.temporaryPassword(document, firstName + " " + lastName, "INSTRUCTOR",
                account.temporaryPassword());
        }
        Instructor instructor = instructorRepository.findByUser_IdUser(account.user().getIdUser()).orElse(null);
        if (instructor == null) {
            instructor = new Instructor();
            instructor.setUser(account.user());
            instructor.setInstructorType(type);
            instructor = instructorRepository.save(instructor);
            summary.created();
            historyService.record("instructor", instructor.getIdInstructor(), ChangeAction.CSV_LOAD,
                    "instructor_type", null, type.name(), null);
        }
        if (type == InstructorType.TRANSVERSAL) {
            if (!programCode.isBlank()) summary.warning("Se ignoró el programa del instructor transversal.");
            return;
        }
        Program program = programRepository.findByProgramCodeIgnoreCase(programCode)
                .orElseThrow(() -> new UserChipException("El programa '" + programCode + "' no existe."));
        if (!instructorProgramRepository.existsByInstructor_IdInstructorAndProgram_IdProgram(
                instructor.getIdInstructor(), program.getIdProgram())) {
            InstructorProgram relation = new InstructorProgram();
            relation.setInstructor(instructor);
            relation.setProgram(program);
            instructorProgramRepository.save(relation);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new UserChipException("El archivo no contiene ninguna fila de datos para procesar.");
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".csv")) throw new UserChipException("El archivo debe tener extensión .csv.");
        if (file.getSize() > MAX_BYTES) throw new UserChipException("El archivo es demasiado grande. El tamaño máximo permitido es 50 MB.");
    }

    private String required(CSVRecord row, Map<String, Integer> headers, String name) {
        String value = value(row, headers, name);
        if (value.isBlank()) throw new UserChipException("El campo '" + name + "' es obligatorio.");
        return value.trim();
    }

    private String value(CSVRecord row, Map<String, Integer> headers, String name) {
        Integer index = headers.get(name);
        return index == null || index >= row.size() ? "" : row.get(index).trim();
    }

    private void validatePerson(String document, String firstName, String lastName, String email) {
        if (!DOCUMENT.matcher(document).matches()) {
            throw new UserChipException("El documento debe tener exactamente 10 dígitos numéricos.");
        }
        validateName(firstName, "nombre");
        validateName(lastName, "apellido");
        if (!EMAIL.matcher(email).matches()) {
            throw new UserChipException("El correo no tiene un formato válido.");
        }
    }

    private void validateName(String value, String field) {
        if (!NAME.matcher(value).matches()) {
            throw new UserChipException("El " + field + " solo puede contener letras y espacios.");
        }
    }

    private void validateProgramCode(String value) {
        if (!PROGRAM_CODE.matcher(value).matches()) {
            throw new UserChipException("El código de programa debe tener entre 2 y 15 caracteres alfanuméricos.");
        }
    }

    private void validateChipCode(String value) {
        if (!CHIP_CODE.matcher(value).matches()) {
            throw new UserChipException("El código de ficha debe tener 7 dígitos numéricos.");
        }
    }
}
