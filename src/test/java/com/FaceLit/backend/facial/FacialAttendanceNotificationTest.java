package com.FaceLit.backend.facial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import com.FaceLit.backend.academic.model.academic.*;
import com.FaceLit.backend.academic.model.enums.AcademicState;
import com.FaceLit.backend.academic.repository.*;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.environment.model.RecordEnvironment;
import com.FaceLit.backend.environment.repository.RecordEnvironmentRepository;
import com.FaceLit.backend.facial.dto.request.UpdateFacialEventExcuseRequestDTO;
import com.FaceLit.backend.facial.model.FacialEvent;
import com.FaceLit.backend.facial.model.enums.*;
import com.FaceLit.backend.facial.repository.*;
import com.FaceLit.backend.facial.service.serviceImpl.FacialEventServiceImpl;
import com.FaceLit.backend.notification.dto.request.CreateNotificationRequestDTO;
import com.FaceLit.backend.notification.service.NotificationCatalog;
import com.FaceLit.backend.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

class FacialAttendanceNotificationTest {
    private final FacialEventRepository events = mock(FacialEventRepository.class);
    private final RecordEnvironmentRepository sessions = mock(RecordEnvironmentRepository.class);
    private final ApprenticeRepository apprentices = mock(ApprenticeRepository.class);
    private final UserChipRepository assignments = mock(UserChipRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final RecordEnvironment session = new RecordEnvironment();
    private final Apprentice apprentice = new Apprentice();
    private final Instructor scheduled = instructor("Ana", "Gómez");
    private final Instructor substitute = instructor("Luis", "Pérez");
    private FacialEventServiceImpl service;
    private UpdateFacialEventExcuseRequestDTO request;

    @BeforeEach
    void setUp() {
        service = new FacialEventServiceImpl(events, sessions, mock(DeviceRepository.class), apprentices,
                assignments, mock(InstructorRepository.class), mock(FacialEmbeddingVerificationClient.class),
                mock(JdbcTemplate.class), notifications);
        apprentice.setIdApprentice(UUID.randomUUID());
        apprentice.setUser(user("Carlos", "Ramírez"));
        Chip chip = new Chip();
        chip.setIdChip(UUID.randomUUID());
        chip.setChipCode("3145555");
        session.setIdRecordEnvironment(UUID.randomUUID());
        session.setChip(chip);
        session.setInstructorScheduled(scheduled);
        session.setInstructorInCharge(substitute);
        // UTC October 13 is still October 12 in Colombia.
        session.setEntryTime(OffsetDateTime.parse("2026-10-13T01:00:00Z"));
        session.setDevice(new com.FaceLit.backend.facial.model.Device());
        request = new UpdateFacialEventExcuseRequestDTO();
        request.setIdRecordEnvironment(session.getIdRecordEnvironment());
        request.setIdApprentice(apprentice.getIdApprentice());
        request.setExcuse(true);
        when(sessions.findById(session.getIdRecordEnvironment())).thenReturn(Optional.of(session));
        when(apprentices.findById(apprentice.getIdApprentice())).thenReturn(Optional.of(apprentice));
        when(assignments.existsByApprentice_IdApprenticeAndChip_IdChipAndState(
                apprentice.getIdApprentice(), chip.getIdChip(), AcademicState.ACTIVE)).thenReturn(true);
        when(events.saveAndFlush(any())).thenAnswer(call -> {
            FacialEvent event = call.getArgument(0);
            event.setIdFacialEvent(UUID.randomUUID());
            return event;
        });
    }

    @Test
    void excuseUsesAbsenceDateAndResponsibleSubstituteForBothRecipients() throws Exception {
        existingAbsence(false);
        service.updateExcuse(request, UUID.randomUUID());
        var captured = ArgumentCaptor.forClass(CreateNotificationRequestDTO.class);
        verify(notifications, times(2)).createForRecipient(captured.capture());
        var sent = captured.getAllValues();
        assertThat(sent).extracting(CreateNotificationRequestDTO::recipientUserId)
                .containsExactly(apprentice.getUser().getIdUser(), substitute.getUser().getIdUser());
        for (var notification : sent) {
            assertThat(notification.type()).isEqualTo("attendance_excuse_registered");
            assertThat(notification.message()).contains("Carlos Ramírez", "12/10/2026", "20:00",
                    "3145555", "Luis Pérez").doesNotContain("Ana Gómez", "13/10/2026");
            var metadata = new ObjectMapper().readTree(notification.metadataJson());
            assertThat(metadata.get("sessionDate").asText()).isEqualTo("2026-10-12");
            assertThat(metadata.get("idInstructor").asText()).isEqualTo(substitute.getIdInstructor().toString());
            assertThat(new NotificationCatalog().require(notification.type()).channel()).isEqualTo("APP_EMAIL");
        }
    }

    @Test
    void newAbsenceIncludesLearnerDateAndScheduledInstructor() {
        session.setInstructorInCharge(null);
        request.setExcuse(false);
        service.updateExcuse(request, UUID.randomUUID());
        var captured = ArgumentCaptor.forClass(CreateNotificationRequestDTO.class);
        verify(notifications).createForRecipient(captured.capture());
        var sent = captured.getValue();
        assertThat(sent.recipientUserId()).isEqualTo(apprentice.getUser().getIdUser());
        assertThat(sent.type()).isEqualTo("attendance_absent");
        assertThat(sent.message()).contains("Carlos Ramírez", "12/10/2026", "3145555", "Ana Gómez");
        assertThat(new NotificationCatalog().require(sent.type()).channel()).isEqualTo("APP_EMAIL");
    }

    @Test
    void repeatingExcuseDoesNotSendDuplicateNotifications() {
        existingAbsence(true);
        service.updateExcuse(request, UUID.randomUUID());
        verifyNoInteractions(notifications);
    }

    @Test
    void excuseCreatingAbsenceSendsOnlyExcuseToLearnerAndInstructor() {
        service.updateExcuse(request, UUID.randomUUID());
        var captured = ArgumentCaptor.forClass(CreateNotificationRequestDTO.class);
        verify(notifications, times(2)).createForRecipient(captured.capture());
        assertThat(captured.getAllValues()).extracting(CreateNotificationRequestDTO::type)
                .containsOnly("attendance_excuse_registered");
    }

    private void existingAbsence(boolean excuse) {
        FacialEvent event = new FacialEvent();
        event.setIdFacialEvent(UUID.randomUUID());
        event.setRecordEnvironment(session);
        event.setDevice(session.getDevice());
        event.setApprentice(apprentice);
        event.setEventDatetime(session.getEntryTime());
        event.setEventType(FacialEventType.ENTRY);
        event.setAttendanceStatus(AttendanceStatus.ABSENT);
        event.setOrigin(FacialEventOrigin.PC);
        event.setExcuse(excuse);
        when(events.findFirstByRecordEnvironment_IdRecordEnvironmentAndApprentice_IdApprenticeAndEventTypeAndDeletedAtIsNullOrderByEventDatetimeAsc(
                eq(session.getIdRecordEnvironment()), eq(apprentice.getIdApprentice()), eq(FacialEventType.ENTRY)))
                .thenReturn(Optional.of(event));
    }

    private static User user(String firstName, String lastName) {
        User user = new User();
        user.setIdUser(UUID.randomUUID());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }

    private static Instructor instructor(String firstName, String lastName) {
        Instructor instructor = new Instructor();
        instructor.setIdInstructor(UUID.randomUUID());
        instructor.setUser(user(firstName, lastName));
        return instructor;
    }
}
