package com.FaceLit.backend.academic.dto.response.academic;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CsvGuideDTO {
    private List<ColumnGuide> columns;
    private List<String> instructions;

    @Getter
    @AllArgsConstructor
    public static class ColumnGuide {
        private String column;
        private String description;
        private String requiredWhen;
    }
}
