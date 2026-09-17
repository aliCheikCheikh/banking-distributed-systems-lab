package com.example.banklab.notification.bootstrap.config;

import com.example.banklab.events.account.BankAccountOpened;
import com.example.banklab.events.EventId;
import com.example.banklab.notification.application.port.out.ProcessedEventStore;
import com.example.banklab.notification.application.port.out.TransactionRunner;
import com.example.banklab.notification.application.port.out.WelcomeNotificationRepository;
import com.example.banklab.notification.application.service.IdempotentBankAccountOpenedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(NotificationProcessingIntegrationTest.TestConfiguration.class)
@Testcontainers
class NotificationProcessingIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("banking_lab_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TransactionRunner transactionRunner;

    @Autowired
    private ProcessedEventStore processedEventStore;

    @Autowired
    private WelcomeNotificationRepository welcomeNotificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpDatabase() {
        jdbcTemplate.update("DELETE FROM notification");
        jdbcTemplate.update("DELETE FROM processed_event");
    }

    @Test
    void should_commit_welcome_notification_and_processed_event_together() {
        BankAccountOpened event = event();
        IdempotentBankAccountOpenedService service = new IdempotentBankAccountOpenedService(
                processedEventStore,
                welcomeNotificationRepository,
                transactionRunner
        );

        assertThat(countProcessedEventRows(event.eventId())).isZero();

        service.process(event);

        Notification notification = loadNotification(event);

        assertThat(notification.id()).isNotNull();
        assertThat(notification.eventId()).isEqualTo(event.eventId().value());
        assertThat(notification.accountId()).isEqualTo(event.accountId());
        assertThat(notification.customerId()).isEqualTo(event.customerId());
        assertThat(notification.type()).isEqualTo("WELCOME");
        assertThat(notification.status()).isEqualTo("PENDING");
        assertThat(notification.createdAt()).isNotNull();
        assertThat(countProcessedEventRows(event.eventId())).isOne();
    }

    @Test
    void should_create_only_one_notification_when_same_event_is_replayed() {
        BankAccountOpened event = event();
        IdempotentBankAccountOpenedService service = service(processedEventStore);

        service.process(event);
        service.process(event);

        assertThat(countNotificationRows(event.eventId())).isOne();
        assertThat(countProcessedEventRows(event.eventId())).isOne();
    }

    @Test
    void should_process_different_events_independently() {
        BankAccountOpened firstEvent = event();
        BankAccountOpened secondEvent = event();
        IdempotentBankAccountOpenedService service = service(processedEventStore);

        service.process(firstEvent);
        service.process(secondEvent);

        assertThat(countNotificationRows(firstEvent.eventId())).isOne();
        assertThat(countNotificationRows(secondEvent.eventId())).isOne();
        assertThat(countProcessedEventRows(firstEvent.eventId())).isOne();
        assertThat(countProcessedEventRows(secondEvent.eventId())).isOne();
    }

    @Test
    void should_create_only_one_notification_when_same_event_is_claimed_concurrently() {
        BankAccountOpened event = event();
        CyclicBarrier claimsReady = new CyclicBarrier(2);
        ProcessedEventStore synchronizedClaims = mock(ProcessedEventStore.class);
        when(synchronizedClaims.tryClaim(any(EventId.class))).thenAnswer(invocation -> {
            await(claimsReady);
            EventId eventId = invocation.getArgument(0);
            return processedEventStore.tryClaim(eventId);
        });
        IdempotentBankAccountOpenedService service = service(synchronizedClaims);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> service.process(event));
            var second = executor.submit(() -> service.process(event));

            assertThatCode(() -> {
                first.get(15, TimeUnit.SECONDS);
                second.get(15, TimeUnit.SECONDS);
            }).doesNotThrowAnyException();
        }

        assertThat(countNotificationRows(event.eventId())).isOne();
        assertThat(countProcessedEventRows(event.eventId())).isOne();
    }

    @Test
    void should_release_claim_when_business_effect_fails() {
        BankAccountOpened event = event();
        AtomicBoolean failFirstAttempt = new AtomicBoolean(true);
        WelcomeNotificationRepository failingOnceRepository = consumedEvent -> {
            welcomeNotificationRepository.createFor(consumedEvent);
            if (failFirstAttempt.getAndSet(false)) {
                throw new IllegalStateException("business effect failed");
            }
        };
        IdempotentBankAccountOpenedService service = new IdempotentBankAccountOpenedService(
                processedEventStore,
                failingOnceRepository,
                transactionRunner
        );

        assertThatThrownBy(() -> service.process(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("business effect failed");

        assertThat(countNotificationRows(event.eventId())).isZero();
        assertThat(countProcessedEventRows(event.eventId())).isZero();

        service.process(event);

        assertThat(countNotificationRows(event.eventId())).isOne();
        assertThat(countProcessedEventRows(event.eventId())).isOne();
    }

    private IdempotentBankAccountOpenedService service(ProcessedEventStore eventStore) {
        return new IdempotentBankAccountOpenedService(
                eventStore,
                welcomeNotificationRepository,
                transactionRunner
        );
    }

    private Notification loadNotification(BankAccountOpened event) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT id, event_id, account_id, customer_id, type, status, created_at
                        FROM notification
                        WHERE event_id = ?
                        """,
                (resultSet, rowNumber) -> new Notification(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("event_id", UUID.class),
                        resultSet.getObject("account_id", UUID.class),
                        resultSet.getObject("customer_id", UUID.class),
                        resultSet.getString("type"),
                        resultSet.getString("status"),
                        resultSet.getObject("created_at", OffsetDateTime.class)
                ),
                event.eventId().value()
        );
    }

    private int countNotificationRows(EventId eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE event_id = ?",
                Integer.class,
                eventId.value()
        );
        return count == null ? 0 : count;
    }

    private int countProcessedEventRows(EventId eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_event WHERE event_id = ?",
                Integer.class,
                eventId.value()
        );
        return count == null ? 0 : count;
    }

    private BankAccountOpened event() {
        return new BankAccountOpened(
                EventId.generate(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "EUR",
                Instant.parse("2026-08-13T10:15:30Z")
        );
    }

    private void await(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError("Concurrent claims did not reach the synchronization point", exception);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(NotificationPersistenceConfiguration.class)
    static class TestConfiguration {
    }

    private record Notification(UUID id,
                                UUID eventId,
                                UUID accountId,
                                UUID customerId,
                                String type,
                                String status,
                                OffsetDateTime createdAt) {
    }
}
