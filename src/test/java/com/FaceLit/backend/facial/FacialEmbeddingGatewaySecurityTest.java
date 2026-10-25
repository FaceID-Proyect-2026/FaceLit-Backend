package com.FaceLit.backend.facial;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;
import com.FaceLit.backend.auth.service.roleandpermission.JwtService;
import com.FaceLit.backend.config.JwtFilter;
import com.FaceLit.backend.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FacialEmbeddingGatewayController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class FacialEmbeddingGatewaySecurityTest {
    @Autowired MockMvc mvc;
    @MockBean FacialEmbeddingVerificationClient client;
    @MockBean JwtService jwtService;
    @MockBean com.FaceLit.backend.realtime.RealtimeWebSocketHandler realtimeWebSocketHandler;

    private UsernamePasswordAuthenticationToken actor(String role, UUID id) {
        return new UsernamePasswordAuthenticationToken(id, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test void anonymousAndOtherRolesCannotEnroll() throws Exception {
        String path = "/api/facial/embeddings/from-image";
        mvc.perform(post(path).contentType("application/json").content("{\"image_base64\":\"image\"}"))
                .andExpect(status().isForbidden());
        for (String role : List.of("INSTRUCTOR", "COORDINATOR")) {
            mvc.perform(post(path).with(authentication(actor(role, UUID.randomUUID())))
                    .contentType("application/json").content("{\"image_base64\":\"image\"}"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(client);
    }

    @Test void forgedUserIdAndActorAreIgnored() throws Exception {
        UUID owner = UUID.randomUUID();
        when(client.registerFromImage(anyMap())).thenReturn(new ObjectMapper().readTree(
                "{\"status\":\"ACTIVE\",\"embedding\":[0.1]}"));
        mvc.perform(post("/api/facial/embeddings/from-image")
                .with(authentication(actor("APPRENTICE", owner)))
                .contentType("application/json")
                .content("{\"image_base64\":\"image\",\"user_id\":\"" + UUID.randomUUID()
                        + "\",\"created_by\":\"attacker\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.embedding").doesNotExist());
        verify(client).registerFromImage(argThat(body -> owner.equals(body.get("user_id"))
                && owner.toString().equals(body.get("created_by"))));
    }

    @Test void livenessRemainsAvailableToApprenticesAndInstructors() throws Exception {
        when(client.checkLiveness(anyMap())).thenReturn(new ObjectMapper().readTree("{\"live\":true,\"reason\":\"LIVE\"}"));
        for (String role : List.of("APPRENTICE", "INSTRUCTOR")) {
            mvc.perform(post("/api/facial/embeddings/liveness-check")
                    .with(authentication(actor(role, UUID.randomUUID())))
                    .contentType("application/json").content("{\"image_frames\":[\"frame\"],\"liveness_challenge\":\"blink\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.live").value(true));
        }
    }
}
