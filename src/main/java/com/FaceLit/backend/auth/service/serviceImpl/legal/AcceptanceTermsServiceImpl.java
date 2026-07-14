package com.FaceLit.backend.auth.service.serviceImpl.legal;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.dto.request.legal.AcceptanceTermsRequestDTO;
import com.FaceLit.backend.auth.dto.response.legal.AcceptanceTermsResponseDTO;
import com.FaceLit.backend.auth.exception.AcceptanceTermsException;
import com.FaceLit.backend.auth.model.legal.AcceptanceTerms;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.repository.legal.AcceptanceTermsRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.service.legal.AcceptanceTermsService;

import jakarta.transaction.Transactional;

@Service
public class AcceptanceTermsServiceImpl implements AcceptanceTermsService {

    private final AcceptanceTermsRepository acceptanceTermsRepository;
    private final UserRepository userRepository;

    public AcceptanceTermsServiceImpl(
            UserRepository userRepository,
            AcceptanceTermsRepository acceptanceTermsRepository) {
        this.userRepository = userRepository;
        this.acceptanceTermsRepository = acceptanceTermsRepository;
    }

    @Override
    @Transactional
    public AcceptanceTermsResponseDTO acceptanceTerms(AcceptanceTermsRequestDTO dto) {

        // 1. Buscar el usuario por UUID
        User user = userRepository.findById(dto.getId_user())
                .orElseThrow(() -> new AcceptanceTermsException("Usuario no encontrado"));

        // 2. Si el usuario no aceptó → rechazar inmediatamente
        if (!dto.getAccepted()) {
            throw new AcceptanceTermsException(
                    "No puede continuar sin confirmar lectura o aceptar responsabilidad");
        }

        // 3. Verificar que no haya aceptado antes — evitar duplicados
        if (acceptanceTermsRepository.existsByUser(user)) {
            throw new AcceptanceTermsException(
                    "El usuario ya aceptó los términos anteriormente");
        } //  este if también cierra aquí

        // 4. Construir y guardar el registro
        AcceptanceTerms acceptanceTerms = new AcceptanceTerms();
        acceptanceTerms.setUser(user);
        acceptanceTerms.setAccepted(true);
        acceptanceTermsRepository.save(acceptanceTerms);

        // 5. Responder con éxito
        return AcceptanceTermsResponseDTO.accepted();
    }
}
