package com.FaceLit.backend.facial;

import com.FaceLit.backend.facial.service.FacialCapabilities;
import com.FaceLit.backend.facial.service.FacialRegistrationService;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Read/confirmation API for the active FastAPI enrollment flow. */
@RestController
@RequestMapping("/api/facial")
public class FacialEnrollmentController {
    private final FacialRegistrationService service;
    private final FacialCapabilities capabilities;

    public FacialEnrollmentController(FacialRegistrationService service, FacialCapabilities capabilities) {
        this.service = service;
        this.capabilities = capabilities;
    }

    @GetMapping("/me")
    public Map<String, Boolean> status(@AuthenticationPrincipal UUID userId) {
        return Map.of("registered", service.isRegistered(userId), "padAvailable", capabilities.legacyPadConfigured());
    }

    @GetMapping("/registration-status")
    public FacialRegistrationService.RegistrationStatus registrationStatus(@AuthenticationPrincipal UUID userId) {
        return service.status(userId);
    }

    @PostMapping("/registration-confirmation")
    public Map<String, Boolean> confirmRegistration(@AuthenticationPrincipal UUID userId) {
        return service.confirm(userId);
    }
}
