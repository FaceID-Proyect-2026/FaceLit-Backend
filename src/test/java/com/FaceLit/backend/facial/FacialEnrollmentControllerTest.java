package com.FaceLit.backend.facial;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

class FacialEnrollmentControllerTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final TransactionTemplate tx = mock(TransactionTemplate.class);
    private final FacialEnrollmentController controller = new FacialEnrollmentController(jdbc, tx);
    private final UUID user = UUID.randomUUID();

    private List<FacialEnrollmentController.Sample> samples(FacialEnrollmentController.Challenge challenge) {
        return new ArrayList<>(challenge.poses().stream().map(pose -> new FacialEnrollmentController.Sample(
            pose, pose.equals("center") ? 0 : pose.equals("left") ? 0.4 : -0.4,
            0.95, 0.95, Collections.nCopies(1024, 0.1))).toList());
    }

    @Test void acceptsCompleteSequenceAndPersistsOnlyOnce() {
        doAnswer(call -> {
            Consumer<TransactionStatus> action = call.getArgument(0);
            action.accept(mock(TransactionStatus.class)); return null;
        }).when(tx).executeWithoutResult(any());
        var challenge = controller.challenge(user);
        var request = new FacialEnrollmentController.Enrollment(challenge.id(), samples(challenge));
        assertTrue(controller.enroll(user, request).get("registered"));
        verify(jdbc).update(contains("INSERT INTO facialrecognition.user_face"), eq(user),
            argThat((byte[] template) -> template.length == 20484), eq(user.toString()));
        assertThrows(ResponseStatusException.class, () -> controller.enroll(user, request));
    }

    @Test void challengeCannotBeUsedByAnotherUser() {
        var challenge = controller.challenge(user);
        assertThrows(ResponseStatusException.class, () -> controller.enroll(UUID.randomUUID(),
            new FacialEnrollmentController.Enrollment(challenge.id(), samples(challenge))));
        verifyNoInteractions(tx);
    }

    @Test void newChallengeInvalidatesOldChallenge() {
        var old = controller.challenge(user);
        controller.challenge(user);
        assertThrows(ResponseStatusException.class, () -> controller.enroll(user,
            new FacialEnrollmentController.Enrollment(old.id(), samples(old))));
        verifyNoInteractions(tx);
    }

    @Test void rejectsStationaryPhotoDespiteHighScores() {
        var challenge = controller.challenge(user);
        var still = samples(challenge).stream().map(s -> new FacialEnrollmentController.Sample(s.pose(), 0, s.real(), s.live(), s.embedding())).toList();
        assertThrows(ResponseStatusException.class, () -> controller.enroll(user,
            new FacialEnrollmentController.Enrollment(challenge.id(), still)));
        verifyNoInteractions(tx);
    }

    @Test void rejectsSpoofScoresAndConsumesChallenge() {
        var challenge = controller.challenge(user);
        var samples = samples(challenge);
        samples.set(0, new FacialEnrollmentController.Sample("center", 0, 0.3, 0.95, samples.get(0).embedding()));
        var request = new FacialEnrollmentController.Enrollment(challenge.id(), samples);
        assertThrows(ResponseStatusException.class, () -> controller.enroll(user, request));
        assertThrows(ResponseStatusException.class, () -> controller.enroll(user, request));
        verifyNoInteractions(tx);
    }

    @Test void rejectsInvalidOrDifferentIdentityEmbedding() {
        var challenge = controller.challenge(user);
        for (double value : new double[] { 0, Double.NaN, Double.POSITIVE_INFINITY, -0.1 }) {
            var samples = samples(challenge);
            var previous = samples.get(1);
            samples.set(1, new FacialEnrollmentController.Sample(previous.pose(), previous.yaw(), 0.95, 0.95,
                Collections.nCopies(1024, value)));
            assertThrows(ResponseStatusException.class, () -> FacialEnrollmentController.validateSamples(challenge, samples));
        }
    }

    @Test void rejectsMissingOrOutOfOrderSteps() {
        var challenge = new FacialEnrollmentController.Challenge(UUID.randomUUID(),
            List.of("center", "left", "center", "right", "center"), Instant.now().plusSeconds(90));
        var samples = samples(challenge);
        Collections.swap(samples, 1, 3);
        assertThrows(ResponseStatusException.class, () -> FacialEnrollmentController.validateSamples(challenge, samples));
        samples.remove(0);
        assertThrows(ResponseStatusException.class, () -> FacialEnrollmentController.validateSamples(challenge, samples));
    }

    @Test void existingRegistrationCannotBeOverwritten() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(user))).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> controller.challenge(user));
        verifyNoInteractions(tx);
    }
}
