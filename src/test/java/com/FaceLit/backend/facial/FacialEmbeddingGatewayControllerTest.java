package com.FaceLit.backend.facial;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

class FacialEmbeddingGatewayControllerTest {
    private final FacialEmbeddingVerificationClient client = mock(FacialEmbeddingVerificationClient.class);
    private final FacialEmbeddingGatewayController controller = new FacialEmbeddingGatewayController(client);
    private final UUID owner = UUID.randomUUID();

    @Test void registrationBindsOwnerPreservesResetAndNeverReturnsVector() throws Exception {
        UUID grant = UUID.randomUUID();
        when(client.registerFromImage(anyMap())).thenReturn(new ObjectMapper().readTree(
                "{\"id\":\"saved\",\"embedding\":[0.5],\"status\":\"ACTIVE\"}"));
        var response = controller.register(owner, new FacialEmbeddingGatewayController.Registration(
                "image", List.of("frame"), "blink", List.of("blink"), null, true, grant));
        assertEquals(201, response.getStatusCode().value());
        assertFalse(response.getBody().has("embedding"));
        assertEquals("ACTIVE", response.getBody().get("status").asText());
        verify(client).registerFromImage(argThat(body -> owner.equals(body.get("user_id"))
                && owner.toString().equals(body.get("created_by")) && grant.equals(body.get("reset_token"))
                && Boolean.TRUE.equals(body.get("replace_existing"))));
    }

    @Test void anonymousRequestsCannotReachMicroservice() {
        assertThrows(ResponseStatusException.class, () -> controller.liveness(null,
                new FacialEmbeddingGatewayController.Liveness(List.of(), "blink")));
        assertThrows(ResponseStatusException.class, () -> controller.register(null, null));
        verifyNoInteractions(client);
    }

    @Test void livenessPreservesResultAndFiltersOtherFields() throws Exception {
        when(client.checkLiveness(anyMap())).thenReturn(new ObjectMapper().readTree(
                "{\"live\":true,\"reason\":\"LIVE\",\"embedding\":[0.5]}"));
        var response = controller.liveness(owner, new FacialEmbeddingGatewayController.Liveness(List.of("frame"), "blink"));
        assertTrue(response.get("live").asBoolean());
        assertFalse(response.has("embedding"));
    }

    @Test void invalidServiceCredentialDoesNotExpireUserSession() {
        when(client.checkLiveness(anyMap())).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));
        var error = assertThrows(ResponseStatusException.class, () -> controller.liveness(owner,
                new FacialEmbeddingGatewayController.Liveness(List.of(), "blink")));
        assertEquals(502, error.getStatusCode().value());
    }

    @Test void noProxyForArbitraryReadUpdateOrDelete() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        String path = "/api/facial/embeddings/users/" + owner;
        mvc.perform(get(path)).andExpect(status().isNotFound());
        mvc.perform(delete(path)).andExpect(status().isNotFound());
        mvc.perform(patch(path)).andExpect(status().isNotFound());
        verifyNoInteractions(client);
    }

    @Test void serviceConfigurationRejectsEmptyOrMissingKey() {
        for (String key : new String[] {null, "", "   "}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new FacialEmbeddingVerificationClient("http://localhost:8090", key));
        }
    }
}
