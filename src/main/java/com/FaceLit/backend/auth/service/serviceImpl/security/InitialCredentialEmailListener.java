package com.FaceLit.backend.auth.service.serviceImpl.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.FaceLit.backend.auth.service.security.InitialCredentialsEmailRequested;
import com.FaceLit.backend.shared.service.EmailService;

@Service
public class InitialCredentialEmailListener {

    private static final Logger log = LoggerFactory.getLogger(InitialCredentialEmailListener.class);

    private final EmailService emailService;
    private final String accessUrl;

    public InitialCredentialEmailListener(
            EmailService emailService,
            @Value("${facelit.access-url:http://localhost:8081}") String accessUrl) {
        this.emailService = emailService;
        this.accessUrl = accessUrl;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInitialCredentialsEmailRequested(InitialCredentialsEmailRequested event) {
        try {
            emailService.sendInitialCredentials(
                    event.email(),
                    "FaceLit - Credenciales iniciales",
                    buildBody(event));
        } catch (Exception ex) {
            log.error("Fallo envio de credenciales iniciales a {}: {}", event.email(), ex.getMessage());
        }
    }

    private String buildBody(InitialCredentialsEmailRequested event) {
        String academicRows = "";
        if (hasText(event.fichaCode())) {
            academicRows += detailRow("Ficha", event.fichaCode());
        }
        if (hasText(event.programName())) {
            academicRows += detailRow("Programa", event.programName());
        }

        return """
                <!doctype html>
                <html lang="es">
                <body style="margin:0;padding:0;background:#edf4f1;font-family:Arial,Helvetica,sans-serif;color:#123026;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#edf4f1;padding:28px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 18px 45px rgba(0,79,48,.22);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#023b27 0%%,#087a4f 55%%,#16c98d 100%%);padding:30px 24px;text-align:center;">
                              <div style="display:inline-block;width:86%%;max-width:390px;border-radius:28px;padding:16px 20px;background:rgba(255,255,255,.13);box-shadow:0 16px 36px rgba(0,0,0,.28),0 0 34px rgba(25,255,174,.35);">
                                <img src="cid:facelitLogo" width="340" alt="FaceLit" style="display:block;width:100%%;max-width:340px;height:auto;margin:0 auto;filter:drop-shadow(0 10px 18px rgba(0,0,0,.35));">
                              </div>
                              <h1 style="margin:18px 0 6px;font-size:28px;line-height:1.15;color:#ffffff;font-weight:800;">Credenciales iniciales</h1>
                              <p style="margin:0;color:#dbfff2;font-size:15px;">Tu acceso a FaceLit ya está listo</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:30px 26px 26px;">
                              <p style="margin:0 0 16px;font-size:18px;line-height:1.5;">Hola <strong>%s</strong>,</p>
                              <p style="margin:0 0 22px;font-size:15px;line-height:1.6;color:#36584d;">Tu cuenta fue creada correctamente. Usa estos datos para iniciar sesión por primera vez.</p>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:separate;border-spacing:0 10px;">
                                %s
                                %s
                                %s
                                %s
                              </table>
                              <div style="margin:22px 0;padding:18px;border-radius:18px;background:#f0fff8;border:1px solid #b9f4da;">
                                <div style="font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#087a4f;font-weight:800;margin-bottom:8px;">Contraseña inicial</div>
                                <div style="font-size:26px;line-height:1.2;letter-spacing:1.5px;color:#023b27;font-weight:900;font-family:'Courier New',monospace;">%s</div>
                              </div>
                              <div style="text-align:center;margin:26px 0 20px;">
                                <a href="%s" style="display:inline-block;background:#087a4f;color:#ffffff;text-decoration:none;border-radius:999px;padding:14px 26px;font-size:15px;font-weight:800;box-shadow:0 10px 20px rgba(8,122,79,.25);">Ingresar a FaceLit</a>
                              </div>
                              <p style="margin:0;color:#58746a;font-size:13px;line-height:1.6;text-align:center;">Inicia sesión con tu número de documento y esta contraseña inicial. Por seguridad, cámbiala después del primer ingreso.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f7fbf9;padding:16px 24px;text-align:center;color:#789188;font-size:12px;">
                              FaceLit · Gestión académica segura
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escape(safe(event.firstName())),
                detailRow("Nombre", fullName(event)),
                detailRow("Rol", event.role()),
                detailRow("Usuario de inicio de sesión", event.loginIdentifier()),
                academicRows,
                escape(event.initialPassword()),
                escape(accessUrl));
    }

    private String detailRow(String label, String value) {
        if (!hasText(value)) {
            return "";
        }
        return """
                <tr>
                  <td style="padding:13px 14px;border-radius:14px;background:#f7fbf9;border:1px solid #e1eee9;">
                    <div style="font-size:12px;color:#6c887d;text-transform:uppercase;letter-spacing:.06em;font-weight:800;">%s</div>
                    <div style="margin-top:4px;font-size:16px;color:#123026;font-weight:700;">%s</div>
                  </td>
                </tr>
                """.formatted(escape(label), escape(value));
    }

    private String fullName(InitialCredentialsEmailRequested event) {
        return (safe(event.firstName()) + " " + safe(event.lastName())).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
