package com.FaceLit.backend.academic.service.serviceImpl.academic;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import com.FaceLit.backend.academic.dto.response.academic.CsvUploadResponseDTO;
import com.FaceLit.backend.academic.repository.ChipRepository;
import com.FaceLit.backend.academic.repository.InstructorProgramRepository;
import com.FaceLit.backend.academic.repository.InstructorRepository;
import com.FaceLit.backend.academic.repository.ProgramRepository;
import com.FaceLit.backend.academic.repository.UserChipRepository;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class CsvAcademicServiceImplTest {

    @Autowired
    private CsvAcademicServiceImpl csvAcademicService;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private ChipRepository chipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private InstructorProgramRepository instructorProgramRepository;

    @Autowired
    private UserChipRepository userChipRepository;

    @Test
    void upload_shouldCreateProgramChipInstructorAndApprenticeWithPassword() throws Exception {
        String csv = """
            tipo,documento,nombre,apellido,correo,programa_codigo,ficha_codigo,instructor_tipo
            programa,,,,,ADSO,,
            ficha,,,,,ADSO,2825551,
            aprendiz,1002345678,Juan,Perez,juan.perez@correo.com,,2825551,
            instructor,1029384756,Laura,Gomez,laura.gomez@correo.com,ADSO,,especifico
            """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "csv-upload.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        CsvUploadResponseDTO response = csvAcademicService.upload(file);

        assertThat(programRepository.findByProgramCodeIgnoreCase("ADSO")).isPresent();
        assertThat(chipRepository.findByChipCode("2825551")).isPresent();
        assertThat(userRepository.findByDocumentNumber("1002345678")).isPresent();
        assertThat(instructorRepository.findAll()).hasSize(1);
        assertThat(instructorProgramRepository.findAll()).hasSize(1);
        assertThat(userChipRepository.findAll()).hasSize(1);
        assertThat(credentialRepository.findByUser_DocumentNumber("1002345678")).isPresent();
        assertThat(response.getContrasenasGeneradas())
                .anySatisfy(item -> {
                    assertThat(item.documento()).isEqualTo("1002345678");
                    assertThat(item.contrasenaTemporal()).isNotBlank();
                    assertThat(item.contrasenaTemporal()).hasSizeGreaterThanOrEqualTo(8);
                });
    }
}
