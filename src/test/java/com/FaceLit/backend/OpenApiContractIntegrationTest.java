package com.FaceLit.backend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "jwt.secret=openapi-test-only-signing-secret-at-least-32-characters",
        "spring.mail.username=test@example.invalid", "spring.mail.password=unused"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void contractIsRestrictedToCoordinator() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs").with(user("learner").roles("APPRENTICE")))
                .andExpect(status().isForbidden());
    }

    @Test void contractDescribesActualCaptureAndAuthenticationRoutes() throws Exception {
        mvc.perform(get("/v3/api-docs").with(user("coordinator").roles("COORDINATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/facial/embeddings/from-image'].post.requestBody").exists())
                .andExpect(jsonPath("$.components.schemas.Liveness.properties.image_frames.minItems").value(3))
                .andExpect(jsonPath("$.components.schemas.Liveness.properties.image_frames.maxItems").value(18))
                .andExpect(jsonPath("$.paths['/api/facial/events/from-image'].post").exists())
                .andExpect(jsonPath("$.paths['/api/facial/enrollment']").doesNotExist());
    }
}
