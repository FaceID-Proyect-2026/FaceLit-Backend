package com.FaceLit.backend.academic.service.serviceImpl.academic;

import com.FaceLit.backend.academic.repository.academic.ChipRepository;
import com.FaceLit.backend.academic.repository.academic.ProgramRepository;
import com.FaceLit.backend.academic.repository.academic.UserChipRepository;
import com.FaceLit.backend.academic.dto.request.academic.ChipRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.ChipResponseDTO;
import com.FaceLit.backend.academic.exception.ChipException;
import com.FaceLit.backend.academic.exception.ProgramException;
import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.model.academic.Program;
import com.FaceLit.backend.academic.model.enums.ChipState;
import com.FaceLit.backend.academic.service.academic.ChipService;
import com.FaceLit.backend.environments.repository.environment.ChipEnvironmentRepository;
import com.FaceLit.backend.schedule.repository.schedule.ScheduleRepository;
import com.FaceLit.backend.schedule.repository.schedule.ScheduleExceptionRepository;
import com.FaceLit.backend.schedule.repository.schedule.ScheduleInstructorRepository;
import com.FaceLit.backend.environments.repository.environment.RecordEnvironmentRepository;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChipServiceImpl implements ChipService {

    private final ChipRepository chipRepository;
    private final ProgramRepository programRepository;
    private final UserChipRepository userChipRepository;
    private final ChipEnvironmentRepository chipEnvironmentRepository;
    private final ScheduleRepository scheduleRepository;
        private final ScheduleExceptionRepository scheduleExceptionRepository;
        private final ScheduleInstructorRepository scheduleInstructorRepository;
        private final RecordEnvironmentRepository recordEnvironmentRepository;

    public ChipServiceImpl(
            ChipRepository chipRepository,
            ProgramRepository programRepository,
            UserChipRepository userChipRepository,
                        ChipEnvironmentRepository chipEnvironmentRepository,
                        ScheduleRepository scheduleRepository,
                        ScheduleExceptionRepository scheduleExceptionRepository,
                        ScheduleInstructorRepository scheduleInstructorRepository,
                        RecordEnvironmentRepository recordEnvironmentRepository) {
        this.chipRepository = chipRepository;
        this.programRepository = programRepository;
        this.userChipRepository = userChipRepository; 
        this.chipEnvironmentRepository = chipEnvironmentRepository; 
        this.scheduleRepository = scheduleRepository; 
                this.scheduleExceptionRepository = scheduleExceptionRepository;
                this.scheduleInstructorRepository = scheduleInstructorRepository;
                this.recordEnvironmentRepository = recordEnvironmentRepository;
    }

    // Genera codigo alfanumerico de 8 caracteres en mayusculas
    // Ejemplo: A3F9K2M7
    // Se regenera si ya existe otro con el mismo codigo — garantiza unicidad
    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
        } while (chipRepository.existsByChipCode(code));
        return code;
    }

    // Convierte entidad a DTO — reutilizado en todos los metodos
    private ChipResponseDTO toDTO(Chip chip, String message) {
        return new ChipResponseDTO(
                chip.getIdChip(),
                chip.getChipCode(),
                chip.getChipName(),
                chip.getWorkingDay(),
                chip.getState(),
                chip.getProgram().getIdProgram(),
                chip.getProgram().getProgramName(),
                message);
    }

    @Override
    @Transactional
    public ChipResponseDTO createChip(ChipRequestDTO dto) {

        // 1. Verificar que el programa existe y esta activo
        Program program = programRepository.findById(dto.getIdProgram())
                .orElseThrow(() -> new ProgramException("Programa no encontrado"));

        // 2. Construir la ficha
        Chip chip = new Chip();
        chip.setProgram(program);
        chip.setChipName(dto.getChipName());
        chip.setWorkingDay(dto.getWorkingDay());
        chip.setState(dto.getState() != null ? dto.getState() : ChipState.ACTIVE);

        // 3. Generar codigo unico automaticamente — el admin no lo ingresa
        chip.setChipCode(generateUniqueCode());

        // 4. Guardar
        Chip saved = chipRepository.save(chip);

        return toDTO(saved, "Ficha registrada correctamente");
    }

    @Override
    @Transactional
    public ChipResponseDTO updateChip(UUID id, ChipRequestDTO dto) {

        // 1. Verificar que la ficha existe
        Chip chip = chipRepository.findById(id)
                .orElseThrow(() -> new ChipException("Ficha no encontrada"));

        // 2. Verificar que el programa existe
        Program program = programRepository.findById(dto.getIdProgram())
                .orElseThrow(() -> new ProgramException("Programa no encontrado"));

        // 2.1 Validar que no se cambie el programa — una ficha pertenece a UN solo
        // programa
        if (!chip.getProgram().getIdProgram().equals(dto.getIdProgram())) {
            throw new ChipException(
                    "No se puede cambiar el programa de una ficha ya registrada");
        }

        // 3. Actualizar campos — el chipCode NO se modifica, fue generado por el
        // sistema
        chip.setProgram(program);
        chip.setChipName(dto.getChipName());
        chip.setWorkingDay(dto.getWorkingDay());
        if (dto.getState() != null) {
            chip.setState(dto.getState());
        }

        Chip updated = chipRepository.save(chip);

        return toDTO(updated, "Ficha actualizada correctamente");
    }

    @Override
    @Transactional
    public void deleteChip(UUID id) {

        Chip chip = chipRepository.findById(id)
                .orElseThrow(() -> new ChipException("Ficha no encontrada"));

        if (chip.getState() == ChipState.INACTIVE) {
            throw new ChipException("La ficha ya está inactiva");
        }

        // Eliminacion logica
        chip.setState(ChipState.INACTIVE);
        chipRepository.save(chip);
    }

    @Override
    public List<ChipResponseDTO> getAllChips() {
        return chipRepository.findAll().stream()
                .map(c -> toDTO(c, null))
                .collect(Collectors.toList());
    }

    @Override
    public ChipResponseDTO getChipById(UUID id) {
        Chip chip = chipRepository.findById(id)
                .orElseThrow(() -> new ChipException("Ficha no encontrada"));
        return toDTO(chip, null);
    }

    @Override
    public List<ChipResponseDTO> getChipsByProgram(UUID idProgram) {
        // Verifica que el programa existe
        programRepository.findById(idProgram)
                .orElseThrow(() -> new ProgramException("Programa no encontrado"));

        return chipRepository.findByProgram_IdProgram(idProgram).stream()
                .map(c -> toDTO(c, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChipResponseDTO> getChipsByState(ChipState state) {
        return chipRepository.findByState(state).stream()
                .map(c -> toDTO(c, null))
                .collect(Collectors.toList());
    }

    @Override
@Transactional
public void permanentDeleteChip(UUID id) {
    Chip chip = chipRepository.findById(id)
            .orElseThrow(() -> new ChipException("Ficha no encontrada"));

    if (chip.getState() == ChipState.ACTIVE) {
        throw new ChipException(
            "La ficha debe estar inactiva antes de eliminarse permanentemente");
    }

    // 1. Eliminar aprendices vinculados
    userChipRepository.findByChip_IdChip(id)
            .forEach(uc -> userChipRepository.delete(uc));

    // 2. Eliminar asignaciones de ambiente
    chipEnvironmentRepository.findByChip_IdChip(id)
            .forEach(ce -> chipEnvironmentRepository.delete(ce));

    // 3. Eliminar horarios y sus dependencias
    scheduleRepository.findByChip_IdChip(id).forEach(schedule -> {
        UUID idSchedule = schedule.getIdSchedule();

        scheduleExceptionRepository
                .findBySchedule_IdSchedule(idSchedule)
                .forEach(ex -> scheduleExceptionRepository.delete(ex));

        scheduleInstructorRepository
                .findBySchedule_IdSchedule(idSchedule)
                .forEach(si -> scheduleInstructorRepository.delete(si));

        recordEnvironmentRepository
                .findAllBySchedule_IdSchedule(idSchedule)
                .forEach(re -> recordEnvironmentRepository.delete(re));

        scheduleRepository.delete(schedule);
    });

    // 4. Eliminar la ficha
    chipRepository.deleteById(id);
}

}
