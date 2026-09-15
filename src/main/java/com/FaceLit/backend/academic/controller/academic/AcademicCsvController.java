package com.FaceLit.backend.academic.controller.academic;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.FaceLit.backend.academic.dto.response.academic.CsvImportSummaryDTO;
import com.FaceLit.backend.academic.dto.response.academic.CsvGuideDTO;
import com.FaceLit.backend.academic.model.academic.PendingTransfer;
import com.FaceLit.backend.academic.service.academic.AcademicCsvService;
import java.util.List;

@RestController
@RequestMapping("/api/coordinator/academic-csv")
public class AcademicCsvController {

    private static final String TEMPLATE = "tipo,documento,nombre,apellido,correo,programa_codigo,ficha_codigo,instructor_tipo\n"
            + "programa,,Análisis y Desarrollo de Software,,,ADSO,,\n"
            + "ficha,,,,,ADSO,2825551,\n"
            + "aprendiz,1002345678,Juan,Perez,juan.perez@correo.com,,2825551,\n"
            + "instructor,1029384756,Laura,Gomez,laura.gomez@correo.com,ADSO,,especifico\n"
            + "instructor,1050607080,Carlos,Ruiz,carlos.ruiz@correo.com,,,transversal\n";

    private final AcademicCsvService academicCsvService;

    public AcademicCsvController(AcademicCsvService academicCsvService) {
        this.academicCsvService = academicCsvService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CsvImportSummaryDTO> importFile(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(academicCsvService.importFile(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("plantilla_gestion_academica.csv").build());
        return ResponseEntity.ok().headers(headers).body(TEMPLATE.getBytes(StandardCharsets.UTF_8));
    }

        @GetMapping("/guide")
        public ResponseEntity<CsvGuideDTO> guide() {
        return ResponseEntity.ok(new CsvGuideDTO(List.of(
            new CsvGuideDTO.ColumnGuide("tipo", "programa, ficha, aprendiz o instructor", "Siempre"),
            new CsvGuideDTO.ColumnGuide("documento", "Documento de la persona", "aprendiz e instructor"),
            new CsvGuideDTO.ColumnGuide("nombre", "Nombre de la persona", "aprendiz e instructor"),
            new CsvGuideDTO.ColumnGuide("apellido", "Apellido de la persona", "aprendiz e instructor"),
            new CsvGuideDTO.ColumnGuide("correo", "Correo para recuperar la contraseña", "aprendiz e instructor"),
            new CsvGuideDTO.ColumnGuide("programa_codigo", "Código del programa", "programa, ficha y específico"),
            new CsvGuideDTO.ColumnGuide("ficha_codigo", "Código de ficha de 7 dígitos", "ficha y aprendiz"),
            new CsvGuideDTO.ColumnGuide("instructor_tipo", "especifico o transversal", "instructor")
        ), List.of(
            "No borres la primera fila con los nombres de columna.",
            "Deja en blanco las columnas que no apliquen; no uses N/A ni guiones.",
            "Puedes subir solo las filas que necesites actualizar.",
            "Revisa los códigos existentes en Gestión Académica antes de armar el archivo."
        )));
        }

    @PostMapping("/pending-transfers/{id}/confirm")
    public ResponseEntity<PendingTransfer> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(academicCsvService.confirmTransfer(id));
    }

    @PostMapping("/pending-transfers/{id}/cancel")
    public ResponseEntity<PendingTransfer> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(academicCsvService.cancelTransfer(id));
    }
}
