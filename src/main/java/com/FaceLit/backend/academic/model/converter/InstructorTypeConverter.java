package com.FaceLit.backend.academic.model.converter;

import com.FaceLit.backend.academic.model.enums.InstructorType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class InstructorTypeConverter implements AttributeConverter<InstructorType, String> {

    @Override
    public String convertToDatabaseColumn(InstructorType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute == InstructorType.CROSS_CUTTING ? "CROSS-CUTTING" : "SPECIFIC";
    }

    @Override
    public InstructorType convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "SPECIFIC" -> InstructorType.SPECIFIC;
            case "CROSS-CUTTING" -> InstructorType.CROSS_CUTTING;
            default -> throw new IllegalArgumentException("Tipo de instructor no soportado: " + value);
        };
    }
}