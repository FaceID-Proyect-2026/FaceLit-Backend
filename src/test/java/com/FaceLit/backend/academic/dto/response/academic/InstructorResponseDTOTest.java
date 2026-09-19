package com.FaceLit.backend.academic.dto.response.academic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.FaceLit.backend.academic.model.academic.Instructor;
import com.FaceLit.backend.academic.model.academic.InstructorProgram;
import com.FaceLit.backend.academic.model.academic.Program;
import com.FaceLit.backend.academic.model.enums.InstructorType;
import com.FaceLit.backend.auth.model.security.User;

class InstructorResponseDTOTest {

    @Test
    void shouldExposeUserIdentityAndPrograms() {
        User user = new User();
        user.setIdUser(UUID.randomUUID());
        user.setDocumentNumber("1029384756");
        user.setFirstName("Laura");
        user.setLastName("Gómez");

        Instructor instructor = new Instructor();
        instructor.setIdInstructor(UUID.randomUUID());
        instructor.setUser(user);
        instructor.setInstructorType(InstructorType.ESPECIFICO);

        Program program = new Program();
        program.setIdProgram(UUID.randomUUID());
        program.setProgramName("ADSO");

        InstructorProgram instructorProgram = new InstructorProgram();
        instructorProgram.setInstructor(instructor);
        instructorProgram.setProgram(program);

        InstructorResponseDTO dto = new InstructorResponseDTO(instructor, List.of(instructorProgram));

        assertEquals("Laura", dto.getFirstName());
        assertEquals("Gómez", dto.getLastName());
        assertEquals("1029384756", dto.getDocument());
        assertEquals("ADSO", dto.getProgramNames().get(0));
    }
}
