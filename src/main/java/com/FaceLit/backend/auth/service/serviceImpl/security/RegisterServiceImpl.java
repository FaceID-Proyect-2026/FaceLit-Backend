package com.FaceLit.backend.auth.service.serviceImpl.security;

import java.util.UUID;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.dto.request.security.RegisterRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.RegisterResponseDTO;
import com.FaceLit.backend.auth.exception.RegisterException;
import com.FaceLit.backend.auth.model.enums.RoleName;
import com.FaceLit.backend.auth.service.security.RegisterService;
import com.FaceLit.backend.auth.service.security.InstitutionalAccountService;

import jakarta.transaction.Transactional;

@Service
public class RegisterServiceImpl implements RegisterService {

    private final InstitutionalAccountService institutionalAccountService;

    public RegisterServiceImpl(InstitutionalAccountService institutionalAccountService) {
        this.institutionalAccountService = institutionalAccountService;
    }

    @Override
    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO dto) {
        InstitutionalAccountService.ProvisionedAccount account = institutionalAccountService.create(
                dto.getFirstName(), dto.getLastName(), dto.getDocumentNumber(), dto.getEmail(), dto.getRole());
        return RegisterResponseDTO.registered(account.user().getIdUser(), account.temporaryPassword());
    }

}
