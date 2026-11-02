package com.FaceLit.backend.facial.service;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Preserves the legacy /me metadata flag without exposing a retired enrollment adapter. */
@Component
public class FacialCapabilities {
    private final boolean legacyPadConfigured;

    public FacialCapabilities(@Value("${facial.pad.url:}") String url,
            @Value("${facial.pad.token:}") String token) {
        boolean configured = false;
        if (!url.isBlank() && !token.isBlank()) {
            try { configured = "https".equals(URI.create(url).getScheme()); }
            catch (IllegalArgumentException ignored) { configured = false; }
        }
        this.legacyPadConfigured = configured;
    }

    public boolean legacyPadConfigured() { return legacyPadConfigured; }
}
