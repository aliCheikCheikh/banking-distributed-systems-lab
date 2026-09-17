package com.example.banklab.notification.infrastructure.out.persistence;

import com.example.banklab.events.account.BankAccountOpened;
import com.example.banklab.notification.application.port.out.WelcomeNotificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;
import java.util.UUID;

public class JdbcWelcomeNotificationRepository implements WelcomeNotificationRepository {
    private static final String WELCOME_TYPE = "WELCOME";
    private static final String PENDING_STATUS = "PENDING";

    private final JdbcTemplate jdbcTemplate;

    public JdbcWelcomeNotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    @Override
    public void createFor(BankAccountOpened event) {
        Objects.requireNonNull(event, "event cannot be null");
        jdbcTemplate.update(
                """
                        INSERT INTO notification (
                            id, event_id, account_id, customer_id, type, status, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                UUID.randomUUID(),
                event.eventId().value(),
                event.accountId(),
                event.customerId(),
                WELCOME_TYPE,
                PENDING_STATUS
        );
    }
}
