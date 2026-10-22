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
            Map.entry("attendance_corrected", new NotificationDefinition("ASISTENCIA", APP_EMAIL, "Asistencia corregida")),
            Map.entry("facial_authorization_expired", new NotificationDefinition("RECONOCIMIENTO_FACIAL", APP, "Permiso facial vencido")),
            Map.entry("user_role_changed", new NotificationDefinition("SEGURIDAD", APP_EMAIL, "Rol de usuario modificado")),
            Map.entry("email_delivery_failed", new NotificationDefinition("SEGURIDAD", APP, "Correo no entregado")),
            Map.entry("csv_upload_done", new NotificationDefinition("ACADEMICO", APP, "Carga de CSV finalizada")),
            Map.entry("csv_inconsistency", new NotificationDefinition("ACADEMICO", APP_EMAIL, "Inconsistencia pendiente de revision")),
            Map.entry("csv_transfer_applied", new NotificationDefinition("TRASLADO", APP, "Cambio de ficha aplicado")),
            Map.entry("csv_transfer_rejected", new NotificationDefinition("TRASLADO", APP, "Cambio de ficha rechazado")),
            Map.entry("csv_ref_error", new NotificationDefinition("ACADEMICO", APP, "Fila con error de referencia")),
            Map.entry("learner_transferred", new NotificationDefinition("TRASLADO", APP_EMAIL, "Traslado de aprendiz")),
            Map.entry("attendance_absent", new NotificationDefinition("ASISTENCIA", APP_EMAIL, "Inasistencia registrada")),
            Map.entry("attendance_late", new NotificationDefinition("ASISTENCIA", APP_EMAIL, "Retraso registrado")),
            Map.entry("attendance_early_exit", new NotificationDefinition("ASISTENCIA", APP_EMAIL, "Salida anticipada")),
            Map.entry("attendance_no_exit", new NotificationDefinition("ASISTENCIA", APP, "Salida no registrada")),
            Map.entry("attendance_wrong_env", new NotificationDefinition("ASISTENCIA", APP_EMAIL, "Registro en sesion no correspondiente")),
            Map.entry("attendance_excuse_registered", new NotificationDefinition("ASISTENCIA", APP_EMAIL, "Excusa registrada")),
            Map.entry("attendance_exit_reminder", new NotificationDefinition("ASISTENCIA", "EMAIL", "Recordatorio de salida")),
            Map.entry("attendance_session_summary", new NotificationDefinition("ASISTENCIA", APP_EMAIL, "Resumen de asistencia")),
            Map.entry("session_assigned", new NotificationDefinition("ASISTENCIA", APP_EMAIL, "Nueva sesion asignada")),
            Map.entry("session_updated", new NotificationDefinition("ASISTENCIA", APP_EMAIL, "Sesion actualizada")),
            Map.entry("session_cancelled", new NotificationDefinition("ASISTENCIA", APP_EMAIL, "Sesion cancelada")),
            Map.entry("attendance_substitute", new NotificationDefinition("ASISTENCIA", APP, "Sesion abierta por suplencia")),
            Map.entry("academic_delete_blocked", new NotificationDefinition("ACADEMICO", APP, "Intento de eliminacion bloqueado")),
            Map.entry("academic_entity_created", new NotificationDefinition("ACADEMICO", APP, "Registro academico creado")),
            Map.entry("academic_entity_updated", new NotificationDefinition("ACADEMICO", APP, "Registro academico actualizado")),
            Map.entry("academic_entity_activated", new NotificationDefinition("ACADEMICO", APP, "Registro academico activado")),
            Map.entry("academic_entity_deactivated", new NotificationDefinition("ACADEMICO", APP, "Registro academico desactivado")),
            Map.entry("academic_entity_deleted", new NotificationDefinition("ACADEMICO", APP, "Registro academico eliminado")),
            Map.entry("user_account_created", new NotificationDefinition("SEGURIDAD", APP, "Tu usuario fue creado")),
            Map.entry("user_profile_updated", new NotificationDefinition("SEGURIDAD", APP_EMAIL, "Tus datos fueron modificados")),
            Map.entry("instructor_profile_updated", new NotificationDefinition("ACADEMICO", APP_EMAIL, "Tus datos fueron modificados")),
            Map.entry("instructor_assignment_updated", new NotificationDefinition("ACADEMICO", APP, "Tu asignacion academica cambio")),
            Map.entry("apprentice_profile_updated", new NotificationDefinition("ACADEMICO", APP_EMAIL, "Tus datos fueron modificados")),
            Map.entry("facial_session_substitution", new NotificationDefinition("RECONOCIMIENTO_FACIAL", APP_EMAIL, "Sesión configurada para otro instructor")),
            Map.entry("facial_request_resolved", new NotificationDefinition("RECONOCIMIENTO_FACIAL", APP_EMAIL, "Respuesta a tu solicitud facial")),
            Map.entry("facial_registration_completed", new NotificationDefinition("RECONOCIMIENTO_FACIAL", APP, "Tu registro facial fue completado")),
            Map.entry("password_changed", new NotificationDefinition("SEGURIDAD", APP_EMAIL, "Tu contrasena fue modificada")),
            Map.entry("account_status_changed", new NotificationDefinition("SEGURIDAD", APP_EMAIL, "Aviso sobre el estado de tu cuenta")),
            Map.entry("apprentice_transfer_applied", new NotificationDefinition("TRASLADO", APP_EMAIL, "Cambio de ficha registrado")),
            Map.entry("security_multiple_failures", new NotificationDefinition("SEGURIDAD", APP_EMAIL, "Multiples intentos fallidos")),
            Map.entry("security_account_locked", new NotificationDefinition("SEGURIDAD", APP_EMAIL, "Cuenta bloqueada")),
            Map.entry("facial_reregister_request", new NotificationDefinition("RECONOCIMIENTO_FACIAL", APP_EMAIL, "Solicitud de re-registro facial"))
    );

    public NotificationDefinition require(String type) {
        NotificationDefinition definition = definitions.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("Tipo de notificacion no soportado: " + type);
        }
        return definition;
    }
}
