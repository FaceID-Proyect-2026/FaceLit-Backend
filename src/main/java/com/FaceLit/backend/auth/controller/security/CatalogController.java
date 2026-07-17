package com.FaceLit.backend.auth.controller.security;

// CATALOGO SE HIZO CON EL FIN DE QUE EL FRONTEND PUEDA CARGAR LOS
//  <select> DEL FORMULARIO DE REGISTRO
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.auth.dto.response.security.DocumentTypeResponseDTO;
import com.FaceLit.backend.auth.service.security.CatalogService;

// Público — se usa en el formulario de registro, antes de tener token
// Ya está permitido en SecurityConfig: "/api/catalogos/**"
@RestController
@RequestMapping("/api/catalogos")
@CrossOrigin(origins = "*")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // GET /api/catalogos/document-types
    @GetMapping("/document-types")
    public List<DocumentTypeResponseDTO> getDocumentTypes() {
        return catalogService.getDocumentTypes();
    }

}
