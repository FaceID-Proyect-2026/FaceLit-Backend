package com.FaceLit.backend.auth.dto.response.security;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

// DTO de solo lectura — no exponemos AuditBase (createdAt, createdBy, etc.)
// el frontend solo necesita id, nombre y abreviación para el <select>
@Getter
@AllArgsConstructor
public class DocumentTypeResponseDTO {

    private UUID idDocumentType;
    private String name;
    private String abbreviation;

}
