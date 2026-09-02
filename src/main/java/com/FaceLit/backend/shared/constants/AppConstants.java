package com.FaceLit.backend.shared.constants;

public final class AppConstants {

 // ── Códigos de verificación ──────────────────────────────
    public static final int VERIFICATION_CODE_LENGTH = 6;
    public static final int VERIFICATION_CODE_MAX = 999999;
    public static final int VERIFICATION_EXPIRY_MINUTES = 5;

    // ── NUEVO — Reenvío de códigos (cooldown) ────────────────
    public static final int RESEND_COOLDOWN_SECONDS = 60;

    // ── NUEVO — Reglas de edad ────────────────────────────────
    public static final int MIN_AGE = 8;
    public static final int MAX_AGE = 100;
    public static final int LEGAL_AGE = 18;

    // ── Códigos de ficha ─────────────────────────────────────
    public static final int CHIP_CODE_LENGTH = 8;

    // ── JWT ──────────────────────────────────────────────────
    public static final String ROLE_PREFIX = "ROLE_";

    // ── Paginación por defecto ───────────────────────────────
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;

    // ── Estados ──────────────────────────────────────────────
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    public static final int JWT_EXPIRY_HOURS = 8;

    private AppConstants() {
        // Evita instanciación — es solo un contenedor de constantes
    }
}
