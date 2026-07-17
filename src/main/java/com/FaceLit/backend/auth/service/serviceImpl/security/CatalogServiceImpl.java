package com.FaceLit.backend.auth.service.serviceImpl.security;

import org.springframework.stereotype.Service;
import java.util.List;
import com.FaceLit.backend.auth.model.security.DocumentType;
import com.FaceLit.backend.auth.dto.response.security.DocumentTypeResponseDTO;
import com.FaceLit.backend.auth.repository.security.DocumentTypeRepository;
import com.FaceLit.backend.auth.service.security.CatalogService;

@Service
public class CatalogServiceImpl implements CatalogService {

    // Reutilizamos el repositorio que ya existe en auth — no se duplica nada
    private final DocumentTypeRepository documentTypeRepository;

    public CatalogServiceImpl(DocumentTypeRepository documentTypeRepository) {
        this.documentTypeRepository = documentTypeRepository;

    }

    @Override
    public List<DocumentTypeResponseDTO> getDocumentTypes() {
        List<DocumentType> types = documentTypeRepository.findAll();

        return types.stream()
                .map(t -> new DocumentTypeResponseDTO(
                        t.getIdDocumentType(),
                        t.getName(),
                        t.getAbbreviation()))
                .toList();
    }

}
