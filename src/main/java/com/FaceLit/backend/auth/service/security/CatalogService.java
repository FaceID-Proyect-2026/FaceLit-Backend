package com.FaceLit.backend.auth.service.security;

import java.util.List;

import com.FaceLit.backend.auth.dto.response.security.DocumentTypeResponseDTO;

public interface CatalogService {

      List<DocumentTypeResponseDTO> getDocumentTypes();

}
