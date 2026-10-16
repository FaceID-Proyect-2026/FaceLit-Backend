package com.FaceLit.backend.shared.service.impl;

import com.FaceLit.backend.auth.service.security.InitialCredentialsEmailRequested;
import com.FaceLit.backend.auth.service.serviceImpl.security.InitialCredentialEmailListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import java.net.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.FaceLit.backend.notification.repository.*;
import com.FaceLit.backend.notification.model.*;
import com.FaceLit.backend.notification.service.EmailDispatchRequested;
import com.FaceLit.backend.notification.service.serviceImpl.EmailDispatchServiceImpl;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.environment.service.ExitReminderService;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionStatus;
import java.util.UUID;
import java.util.Optional;
import java.util.function.Consumer;

class EmailHttpsTest {
    private HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile JsonNode payload;
    private volatile String authorization;
    private volatile int status = 200;
    private volatile long delay;
    private final AtomicInteger requests = new AtomicInteger();
    private EmailServiceImpl service;

    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/emails", exchange -> {
            requests.incrementAndGet();
            authorization = exchange.getRequestHeaders().getFirst("Authorization");
            payload = mapper.readTree(exchange.getRequestBody());
            try {
                Thread.sleep(delay);
                exchange.sendResponseHeaders(status, -1);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        service = new EmailServiceImpl(transport(Duration.ofSeconds(3)));
    }
    private ResendEmailTransport transport(Duration timeout) {
        return new ResendEmailTransport(mapper, "test-key", "sender@example.org", "FaceLit QA",
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/emails"), timeout);
    }
    @AfterEach void stop() { server.stop(0); }

    @Test void welcomePreservesListenerContentLinkAndInlineLogo() throws Exception {
        new InitialCredentialEmailListener(service, "https://facelit.example.org").onInitialCredentialsEmailRequested(
                new InitialCredentialsEmailRequested("qa@example.org", "Ana", "Perez", "LEARNER", "12345", "Test!123", "314", "Programa QA"));
        assertThat(payload).isNotNull();
        assertThat(payload.path("to").get(0).asText()).isEqualTo("qa@example.org");
        assertThat(payload.path("subject").asText()).isEqualTo("FaceLit - Credenciales iniciales");
        assertThat(payload.path("html").asText()).contains("Ana", "Test!123", "314", "Programa QA", "https://facelit.example.org", "cid:facelitLogo");
        assertThat(payload.path("text").asText()).contains("Tu cuenta en FaceLit fue creada");
        assertThat(authorization).isEqualTo("Bearer test-key");
        assertThat(payload.path("from").asText()).isEqualTo("FaceLit QA <sender@example.org>");
        JsonNode logo = payload.path("attachments").get(0);
        assertThat(logo.path("content_id").asText()).isEqualTo("facelitLogo");
        assertThat(logo.path("content_type").asText()).isEqualTo("image/png");
        try (var input = new ClassPathResource("static/email/facelit-logo.png").getInputStream()) {
            assertThat(Base64.getDecoder().decode(logo.path("content").asText())).isEqualTo(input.readAllBytes());
        }
    }
    @Test void recoveryPreservesReceivedCodeAndExpiry() {
        service.sendRecoveryCode("qa@example.org", "012345");
        assertThat(payload.path("subject").asText()).isEqualTo("FaceLit - Codigo de recuperacion de contrasena");
        assertThat(payload.path("html").asText()).contains("012345", "5 minutos");
        assertThat(payload.path("text").asText()).contains("012345", "5 minutos");
    }
    @Test void notificationPreservesSubjectAndText() {
        service.sendNotificationEmail("qa@example.org", "Recordatorio", "Ficha 314\nSalida 18:00");
        assertThat(payload.path("subject").asText()).isEqualTo("Recordatorio");
        assertThat(payload.path("text").asText()).isEqualTo("Ficha 314\nSalida 18:00");
        assertThat(payload.path("html").asText()).contains("Ficha 314<br>Salida 18:00");
    }
    @ParameterizedTest @ValueSource(ints = {302, 400, 401, 403, 429, 500, 503})
    void rejectionFailsWithoutRetry(int rejectedStatus) {
        status = rejectedStatus;
        assertThatThrownBy(() -> service.sendNotificationEmail("qa@example.org", "QA", "Body"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTP " + rejectedStatus);
        assertThat(requests).hasValue(1);
    }
    @Test void responseTimeoutFails() {
        delay = 400;
        service = new EmailServiceImpl(transport(Duration.ofMillis(100)));
        assertThatThrownBy(() -> service.sendRecoveryCode("qa@example.org", "012345"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTPS");
    }
    @Test void httpRejectionIsRecordedAsFailedByExistingDispatcher() {
        status = 403;
        var logs = mock(EmailLogRepository.class);
        var notifications = mock(NotificationRepository.class);
        var credentials = mock(CredentialRepository.class);
        var tx = mock(TransactionTemplate.class);
        var notification = new Notification();
        notification.setIdNotification(UUID.randomUUID());
        notification.setIdUserApp(UUID.randomUUID());
        notification.setType("qa_notification");
        notification.setTitle("QA");
        notification.setMessage("Contenido QA");
        var email = new EmailLog();
        email.setIdNotification(notification.getIdNotification());
        var credential = new Credential();
        credential.setEmail("qa@example.org");
        when(notifications.findById(notification.getIdNotification())).thenReturn(Optional.of(notification));
        when(logs.lockByNotification(notification.getIdNotification())).thenReturn(Optional.of(email));
        when(credentials.findByUser_IdUser(notification.getIdUserApp())).thenReturn(Optional.of(credential));
        doAnswer(call -> {
            Consumer<TransactionStatus> action = call.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(tx).executeWithoutResult(any());
        new EmailDispatchServiceImpl(logs, notifications, credentials, service,
                mock(ExitReminderService.class), tx)
                .onEmailDispatchRequested(new EmailDispatchRequested(notification.getIdNotification()));
        assertThat(email.getSendStatus()).isEqualTo(EmailLog.SendStatus.FAILED);
        verify(logs).save(email);
        assertThat(requests).hasValue(1);
    }
    @Test void springCreatesMailLayerWithoutSmtpAndBindsConfiguration() {
        new ApplicationContextRunner().withBean(ObjectMapper.class, () -> mapper)
                .withUserConfiguration(ResendEmailTransport.class, EmailServiceImpl.class)
                .withPropertyValues("facelit.email.api-key=config-key", "facelit.email.from=qa@example.org", "facelit.email.from-name=QA")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(EmailServiceImpl.class);
                    assertThat(context.getBean(ResendEmailTransport.class)).hasFieldOrPropertyWithValue("apiKey", "config-key")
                            .hasFieldOrPropertyWithValue("from", "QA <qa@example.org>");
                });
    }
    @Test void missingConfigurationAllowsStartupButFailsSending() {
        var unconfigured = new ResendEmailTransport(mapper, "", "", "FaceLit");
        assertThatThrownBy(() -> unconfigured.send("qa@example.org", "QA", "Text", "HTML"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("RESEND_API_KEY");
    }
}
