package com.FaceLit.backend.academic.model.academic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.FaceLit.backend.academic.model.enums.InstructorType;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.shared.model.AuditBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "instructor", schema = "academic")
public class Instructor extends AuditBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_instructor", nullable = false)
    private UUID idInstructor;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user_app", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "instructor_type", nullable = false, length = 20)
    private InstructorType instructorType;

    @OneToMany(mappedBy = "instructor", fetch = FetchType.LAZY)
    private List<InstructorProgram> programs = new ArrayList<>();
}
