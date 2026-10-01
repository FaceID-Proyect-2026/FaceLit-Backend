package com.FaceLit.backend.notification.service;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationCatalog {

    public record NotificationDefinition(String category, String channel, String defaultTitle) {
    }

    private static final String APP = "APP";
    private static final String APP_EMAIL = "APP_EMAIL";

    private final Map<String, NotificationDefinition> definitions = Map.ofEntries(
            Map.entry("csv_upload_done", new NotificationDefinition("ACADEMICO", APP, "Carga de CSV finalizada")),
            Map.entry("csv_inconsistency", new NotificationDefinition("ACADEMICO", APP_EMAIL, "Inconsistencia pendiente de revision")),
            Map.entry("csv_transfer_applied", new NotificationDefinition("TRASLADO", APP, "Cambio de ficha aplicado")),
            Map.entry("csv_transfer_rejected", new NotificationDefinition("TRASLADO", APP, "Cambio de ficha rechazado")),
            Map.entry("csv_ref_error", new NotificationDefinition("ACADEMICO", APP, "Fila con error de referencia")),
            Map.entry("learner_transferred", new NotificationDefinition("TRASLADO", APP, "Traslado de aprendiz")),
            Map.entry("attendance_absent", new NotificationDefinition("ASISTENCIA", APP, "Inasistencia registrada")),
            Map.entry("attendance_late", new NotificationDefinition("ASISTENCIA", APP, "Retraso registrado")),
            Map.entry("attendance_early_exit", new NotificationDefinition("ASISTENCIA", APP, "Salida anticipada")),
            Map.entry("attendance_no_exit", new NotificationDefinition("ASISTENCIA", APP, "Salida no registrada")),
            Map.entry("attendance_wrong_env", new NotificationDefinition("ASISTENCIA", APP_EMAIL, "Registro en sesion no correspondiente")),
            Map.entry("attendance_substitute", new NotificationDefinition("ASISTENCIA", APP, "Sesion abierta por suplencia")),
            Map.entry("academic_delete_blocked", new NotificationDefinition("ACADEMICO", APP, "Intento de eliminacion bloqueado")),
            Map.entry("user_account_created", new NotificationDefinition("SEGURIDAD", APP, "Tu usuario fue creado")),
            Map.entry("user_profile_updated", new NotificationDefinition("SEGURIDAD", APP, "Tus datos fueron modificados")),
            Map.entry("instructor_profile_updated", new NotificationDefinition("ACADEMICO", APP, "Tus datos fueron modificados")),
            Map.entry("instructor_assignment_updated", new NotificationDefinition("ACADEMICO", APP, "Tu asignacion academica cambio")),
            Map.entry("apprentice_profile_updated", new NotificationDefinition("ACADEMICO", APP, "Tus datos fueron modificados")),
            Map.entry("facial_session_substitution", new NotificationDefinition("RECONOCIMIENTO_FACIAL", APP, "Sesion configurada con suplencia")),
            Map.entry("apprentice_transfer_applied", new NotificationDefinition("TRASLADO", APP, "Cambio de ficha registrado")),
            Map.entry("security_multiple_failures", new NotificationDefinition("SEGURIDAD", APP_EMAIL, "Multiples intentos fallidos")),
            Map.entry("security_account_locked", new NotificationDefinition("SEGURIDAD", APP_EMAIL, "Cuenta bloqueada")),
            Map.entry("facial_reregister_request", new NotificationDefinition("RECONOCIMIENTO_FACIAL", APP, "Solicitud de re-registro facial"))
    );

    public NotificationDefinition require(String type) {
        NotificationDefinition definition = definitions.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("Tipo de notificacion no soportado: " + type);
        }
        return definition;
    }
}
