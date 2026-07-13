package com.FaceLit.backend.academic.service.serviceImpl.academic;

import com.FaceLit.backend.academic.dto.request.academic.UserChipRequestDTO;
import com.FaceLit.backend.academic.dto.response.academic.UserChipResponseDTO;
import com.FaceLit.backend.academic.exception.UserChipException;
import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.model.academic.UserChip;
import com.FaceLit.backend.academic.model.enums.ChipState;
import com.FaceLit.backend.academic.model.enums.UserChipStatus;
import com.FaceLit.backend.academic.repository.academic.ChipRepository;
import com.FaceLit.backend.academic.repository.academic.UserChipRepository;
import com.FaceLit.backend.academic.service.academic.UserChipService;
import com.FaceLit.backend.auth.model.security.User;

import com.FaceLit.backend.auth.repository.security.UserRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserChipServiceImpl implements UserChipService {

    private final UserChipRepository userChipRepository;
    private final ChipRepository chipRepository;
    private final UserRepository userRepository;

    public UserChipServiceImpl(
            UserChipRepository userChipRepository,
            ChipRepository chipRepository,
            UserRepository userRepository) {
        this.userChipRepository = userChipRepository;
        this.chipRepository = chipRepository;
        this.userRepository = userRepository;

    }

    // Metodo privado para convertir un UserChip a UserChipResponseDTO
    private UserChipResponseDTO toDTO(UserChip uc, String message) { // Recibe la entidad que viene de la base de datos.
        return new UserChipResponseDTO( // Aquí se está creando un nuevo objeto del DTO usando su constructor.
                uc.getIdUserChip(),
                uc.getUser().getIdUser(),
                uc.getUser().getFirstName() + " " + uc.getUser().getLastName(),
                uc.getChip().getIdChip(),
                uc.getChip().getChipName(),
                uc.getChip().getChipCode(),
                uc.getChip().getProgram().getProgramName(),
                uc.getAssignmentDate(),
                uc.getState().name(),
                message);
    }

    @Override
    @Transactional
    public UserChipResponseDTO joinChip(UUID userId, UserChipRequestDTO dto) {

        // 1. Verificar que el usuario existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserChipException("Usuario no encontrado"));

        // 2. Verificar que el aprendiz no esté ya en una ficha activa
        // Regla de negocio: un aprendiz solo puede estar en UNA ficha activa
        if (userChipRepository.existsByUser_IdUserAndState(userId, UserChipStatus.ACTIVE)) {
            throw new UserChipException(
                    "Ya estás vinculado a una ficha activa. Contacta al administrador para desvincular");
        }

        // 3. Buscar la ficha por codigo
        Chip chip = chipRepository.findByChipCode(dto.getChipCode())
                .orElseThrow(() -> new UserChipException(
                        "El código de ficha no existe o no está activo"));

        // 4. Verificar que la ficha esté ACTIVE
        if (chip.getState() != ChipState.ACTIVE) {
            throw new UserChipException(
                    "La ficha ya no está disponible para nuevos registros");
        }

        // 5. Crear la asignacion
        UserChip userChip = new UserChip();
        userChip.setUser(user);
        userChip.setChip(chip);
        userChip.setAssignmentDate(LocalDate.now());
        userChip.setState(UserChipStatus.ACTIVE);

        UserChip saved = userChipRepository.save(userChip);

        return UserChipResponseDTO.joined(
                saved.getIdUserChip(),
                saved.getUser().getIdUser(),
                saved.getUser().getFirstName() + " " + saved.getUser().getLastName(),
                saved.getChip().getIdChip(),
                saved.getChip().getChipName(),
                saved.getChip().getChipCode(),
                saved.getChip().getProgram().getProgramName(),
                saved.getAssignmentDate());
    }

    @Override
    public List<UserChipResponseDTO> getApprenticesByChip(UUID idChip) {
        return userChipRepository.findByChip_IdChip(idChip).stream()
                .map(uc -> toDTO(uc, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeApprenticeFromChip(UUID idUserChip) {

        UserChip userChip = userChipRepository.findById(idUserChip)
                .orElseThrow(() -> new UserChipException("Asignacion no encontrada"));

        if (userChip.getState() == UserChipStatus.INACTIVE) {
            throw new UserChipException("Esta asignacion ya está inactiva");
        }

        // Eliminacion logica — NO elimina al aprendiz ni la ficha
        // El aprendiz podra unirse a otra ficha despues de esto
        userChip.setState(UserChipStatus.INACTIVE);
        userChipRepository.save(userChip);
    }

    @Override
    public List<UserChipResponseDTO> getChipsByUser(UUID idUser) {
        return userChipRepository.findByUser_IdUser(idUser).stream()
                .map(uc -> toDTO(uc, null))
                .collect(Collectors.toList());
    }

}
