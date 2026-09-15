package com.FaceLit.backend.academic.service.serviceImpl.academic;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.academic.dto.response.academic.ChipResponseDTO;
import com.FaceLit.backend.academic.dto.response.academic.UserChipResponseDTO;
import com.FaceLit.backend.academic.exception.UserChipException;
import com.FaceLit.backend.academic.model.academic.Chip;
import com.FaceLit.backend.academic.model.academic.UserChip;
import com.FaceLit.backend.academic.model.enums.ChangeAction;
import com.FaceLit.backend.academic.model.enums.ChipState;
import com.FaceLit.backend.academic.model.enums.UserChipStatus;
import com.FaceLit.backend.academic.repository.academic.ChipRepository;
import com.FaceLit.backend.academic.repository.academic.UserChipRepository;
import com.FaceLit.backend.academic.service.academic.UserChipService;
import com.FaceLit.backend.academic.service.audit.ChangeHistoryService;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.security.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserChipServiceImpl implements UserChipService {

    private final UserChipRepository userChipRepository;
    private final ChipRepository chipRepository;
    private final UserRepository userRepository;
    private final ChangeHistoryService changeHistoryService;

    public UserChipServiceImpl(UserChipRepository userChipRepository,
            ChipRepository chipRepository,
            UserRepository userRepository,
            ChangeHistoryService changeHistoryService) {
        this.userChipRepository = userChipRepository;
        this.chipRepository = chipRepository;
        this.userRepository = userRepository;
        this.changeHistoryService = changeHistoryService;
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
            throw new UserChipException("Esta asignacion ya estÃ¡ inactiva");
        }

        // Eliminacion logica â€” NO elimina al aprendiz ni la ficha
        // El Coordinador realizara cualquier traslado mediante el flujo administrativo.
        userChip.setState(UserChipStatus.INACTIVE);
        userChipRepository.save(userChip);
        changeHistoryService.record("user_chip", userChip.getUser().getIdUser(), ChangeAction.UPDATE,
            "chip", userChip.getChip().getChipCode(), null, null);
    }

    @Override
    public List<UserChipResponseDTO> getChipsByUser(UUID idUser) {
        return userChipRepository.findByUser_IdUser(idUser).stream()
                .map(uc -> toDTO(uc, null))
                .collect(Collectors.toList());
    }

    @Override
	public List<ChipResponseDTO> getAvailableTransferTargets(UUID idUser) {
	UUID currentChipId = userChipRepository.findByUser_IdUserAndState(idUser, UserChipStatus.ACTIVE)
	    .map(userChip -> userChip.getChip().getIdChip())
	    .orElseThrow(() -> new UserChipException("El aprendiz no tiene una ficha activa para trasladar"));

	return chipRepository.findByState(ChipState.ACTIVE).stream()
	    .filter(chip -> !chip.getIdChip().equals(currentChipId))
	    .map(chip -> new ChipResponseDTO(
	        chip.getIdChip(), chip.getChipCode(), chip.getState(),
            chip.getProgram().getIdProgram(), chip.getProgram().getProgramName(), null,
        chip.getCreatedAt(), chip.getUpdatedAt()))
	    .collect(Collectors.toList());
	}

            @Override
            @Transactional
            public UserChipResponseDTO transferChip(UUID idUser, UUID targetChipId) {
                return transferChip(idUser, targetChipId, ChangeAction.UPDATE);
            }

            @Override
            @Transactional
            public UserChipResponseDTO transferChip(UUID idUser, UUID targetChipId, ChangeAction action) {
            User user = userRepository.findById(idUser)
                .orElseThrow(() -> new UserChipException("Usuario no encontrado"));
            UserChip current = userChipRepository.findByUser_IdUserAndState(idUser, UserChipStatus.ACTIVE)
                .orElseThrow(() -> new UserChipException("El aprendiz no tiene una ficha activa para trasladar"));
            Chip target = chipRepository.findById(targetChipId)
                .orElseThrow(() -> new UserChipException("Ficha destino no encontrada"));

            if (target.getState() != ChipState.ACTIVE) {
                throw new UserChipException("La ficha destino no está activa");
            }
            if (current.getChip().getIdChip().equals(targetChipId)) {
                throw new UserChipException("La ficha destino debe ser diferente a la actual");
            }

            String previousCode = current.getChip().getChipCode();
            current.setState(UserChipStatus.INACTIVE);
            userChipRepository.save(current);

            UserChip replacement = new UserChip();
            replacement.setUser(user);
            replacement.setChip(target);
            replacement.setAssignmentDate(java.time.OffsetDateTime.now());
            replacement.setState(UserChipStatus.ACTIVE);
            UserChip saved = userChipRepository.save(replacement);

            changeHistoryService.record("user_chip", idUser, action, "chip",
                previousCode, target.getChipCode(), null);

            return toDTO(saved, "Aprendiz trasladado correctamente");
            }

            // Metodo privado para convertir un UserChip a UserChipResponseDTO
			private UserChipResponseDTO toDTO(UserChip uc, String message) { // Recibe la entidad que viene de la base de datos.
			    return new UserChipResponseDTO( // AquÃ­ se estÃ¡ creando un nuevo objeto del DTO usando su constructor.
			            uc.getIdUserChip(),
			            uc.getUser().getIdUser(),
			            uc.getUser().getFirstName() + " " + uc.getUser().getLastName(),
			            uc.getUser().getFirstName(),
			            uc.getUser().getLastName(),
			            uc.getUser().getDocumentNumber(),
			            uc.getChip().getIdChip(),
			            uc.getChip().getChipCode(),
			            uc.getChip().getChipCode(),
			            uc.getChip().getProgram().getProgramName(),
			            uc.getAssignmentDate(),
			            uc.getState().name(),
			            message);
			}

}
