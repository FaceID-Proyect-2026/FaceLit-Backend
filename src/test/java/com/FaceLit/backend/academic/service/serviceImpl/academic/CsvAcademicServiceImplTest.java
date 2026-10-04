package com.FaceLit.backend.academic.service.serviceImpl.academic;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
import com.FaceLit.backend.academic.model.academic.Program;
import com.FaceLit.backend.academic.model.enums.AcademicState;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.model.enums.RoleName;
import com.FaceLit.backend.auth.model.roleandpermission.Role;
import com.FaceLit.backend.auth.repository.roleandpermission.RoleRepository;

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

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void seedRequiredRoles() {
        ensureRoleExists(RoleName.INSTRUCTOR);
        ensureRoleExists(RoleName.APPRENTICE);
    }

    private void ensureRoleExists(RoleName roleName) {
        if (roleRepository.findByNameRole(roleName).isEmpty()) {
            Role role = new Role();
            role.setNameRole(roleName);
            roleRepository.save(role);
        }
    }

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

        var program = programRepository.findByProgramCodeIgnoreCase("ADSO").orElseThrow();
        var programId = program.getIdProgram();
        assertThat(program.getCreatedAt()).isNotNull();
        assertThat(program.getUpdatedAt()).isNotNull();
        assertThat(program.getState()).isNotNull();
        assertThat(chipRepository.findByChipCode("2825551")).isPresent();
        assertThat(userRepository.findByDocumentNumber("1002345678")).isPresent();
        var instructorUser = userRepository.findByDocumentNumber("1029384756").orElseThrow();
        assertThat(instructorRepository.findByUser_IdUser(instructorUser.getIdUser())).isPresent();
        assertThat(instructorProgramRepository.findByInstructor_IdInstructor(
                instructorRepository.findByUser_IdUser(instructorUser.getIdUser()).orElseThrow().getIdInstructor()))
                .anySatisfy(relation -> assertThat(relation.getProgram().getIdProgram()).isEqualTo(programId));
        assertThat(userChipRepository.findAll()).hasSize(1);
        assertThat(credentialRepository.findByUser_DocumentNumber("1002345678")).isPresent();
        assertThat(response.getContrasenasGeneradas())
                .anySatisfy(item -> {
                    assertThat(item.documento()).isEqualTo("1002345678");
                    assertThat(item.contrasenaTemporal()).isNotBlank();
                    assertThat(item.contrasenaTemporal()).hasSizeGreaterThanOrEqualTo(8);
                });
        assertThat(response.getCreados()).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void upload_shouldPersistProgramWhenCsvHasUtf8Bom() throws Exception {
        String code = "Q" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
        String csv = "\uFEFFtipo,documento,nombre,apellido,correo,programa_codigo,ficha_codigo,instructor_tipo\r\n"
                + "programa,," + "Programa de prueba,,," + code + ",,\r\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "csv-bom.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        CsvUploadResponseDTO response = csvAcademicService.upload(file);

        assertThat(response.getCreados()).anySatisfy(row -> assertThat(row.detalle()).contains(code));
        assertThat(programRepository.findByProgramCodeIgnoreCase(code)).isPresent();
    }

    @Test
    void template_shouldIncludeCompleteExamplesForEveryRowType() {
        String template = new String(csvAcademicService.template(), StandardCharsets.UTF_8);

        assertThat(template).contains(
                "tipo,documento,nombre,apellido,correo,programa_codigo,ficha_codigo,instructor_tipo",
                "programa,,Análisis y Desarrollo de Software,,,ADSO,,",
                "ficha,,,,,ADSO,2825551,",
                "aprendiz,100234,Juan,Perez,juan.perez@correo.com,,2825551,",
                "instructor,1029384756,Laura,Gomez,laura.gomez@correo.com,ADSO,,especifico",
                "instructor,1050607080,Carlos,Ruiz,carlos.ruiz@correo.com,,,transversal");
    }

    @Test
    void upload_shouldResolveExistingProgramForSpecificInstructor() throws Exception {
        String code = "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
        Program program = new Program();
        program.setProgramCode(code);
        program.setProgramName("Programa existente");
        program.setState(AcademicState.ACTIVE);
        program = programRepository.save(program);
        var programId = program.getIdProgram();
        String csv = """
                tipo,documento,nombre,apellido,correo,programa_codigo,ficha_codigo,instructor_tipo
                instructor,1029384757,Laura,Gomez,laura.gomez@example.com,%s,,especifico
                """.formatted(code);

        CsvUploadResponseDTO response = csvAcademicService.upload(csvFile(csv));

        assertThat(response.getErroresDeReferencia()).isEmpty();
        assertThat(response.getCreados()).anySatisfy(row -> assertThat(row.tipo()).isEqualTo("instructor"));
        assertThat(instructorProgramRepository.findAll())
                .anySatisfy(relation -> assertThat(relation.getProgram().getIdProgram()).isEqualTo(programId));
    }

    @Test
    void upload_shouldReportMissingSpecificInstructorProgramWithoutCreatingRecords() throws Exception {
        String csv = """
                tipo,documento,nombre,apellido,correo,programa_codigo,ficha_codigo,instructor_tipo
                instructor,1029384758,Laura,Gomez,laura.gomez@example.com,NOEXISTE,,especifico
                """;

        CsvUploadResponseDTO response = csvAcademicService.upload(csvFile(csv));

        assertThat(response.getErroresDeReferencia()).hasSize(1);
        assertThat(response.getCreados()).isEmpty();
        assertThat(userRepository.findByDocumentNumber("1029384758")).isEmpty();
    }

    private MockMultipartFile csvFile(String csv) {
        return new MockMultipartFile("file", "csv-upload.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
    }
}
