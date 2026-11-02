package com.FaceLit.backend.facial;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.FaceLit.backend.auth.service.roleandpermission.JwtService;
import com.FaceLit.backend.auth.service.serviceImpl.security.RecoveryRateLimiter;
import com.FaceLit.backend.config.*;
import com.FaceLit.backend.facial.service.FacialCapabilities;
import com.FaceLit.backend.facial.service.FacialRegistrationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(FacialEnrollmentController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class FacialEnrollmentControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean FacialRegistrationService service;
    @MockitoBean FacialCapabilities capabilities;
    @MockitoBean JwtService jwt;
    @MockitoBean RecoveryRateLimiter limiter;
    @MockitoBean com.FaceLit.backend.realtime.RealtimeWebSocketHandler realtime;
    private final UUID actor = UUID.randomUUID();

    private RequestPostProcessor as(String role) {
        return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test void retiredRoutesStayForbiddenForEveryRoleWithoutCallingPersistence() throws Exception {
        for (String role : List.of("APPRENTICE", "INSTRUCTOR", "COORDINATOR")) {
            mvc.perform(get("/api/facial/challenge").with(as(role))).andExpect(status().isForbidden());
            mvc.perform(get("/api/facial/profile-photo").with(as(role))).andExpect(status().isForbidden());
            mvc.perform(post("/api/facial/enrollment").with(as(role))
                    .contentType("application/json").content("{}")).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service, capabilities);
    }

    @Test void currentStatusPreservesResponseFieldsAndAuthenticatedOwner() throws Exception {
        var date = OffsetDateTime.parse("2026-10-27T12:00:00Z");
        when(service.status(actor)).thenReturn(new FacialRegistrationService.RegistrationStatus(true, date));
        when(service.isRegistered(actor)).thenReturn(true);
        when(capabilities.legacyPadConfigured()).thenReturn(false);
        mvc.perform(get("/api/facial/registration-status").with(as("APPRENTICE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.registrationDate").exists());
        mvc.perform(get("/api/facial/me").with(as("APPRENTICE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.padAvailable").value(false));
        verify(service).status(actor);
    }

    @Test void confirmationUsesThePrincipalAndPreservesItsResponse() throws Exception {
        when(service.confirm(actor)).thenReturn(Map.of("confirmed", true));
        mvc.perform(post("/api/facial/registration-confirmation").with(as("APPRENTICE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.confirmed").value(true));
        verify(service).confirm(actor);
    }

    @Test void activeRoutesStillRequireAuthentication() throws Exception {
        mvc.perform(get("/api/facial/registration-status")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/facial/registration-confirmation")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
