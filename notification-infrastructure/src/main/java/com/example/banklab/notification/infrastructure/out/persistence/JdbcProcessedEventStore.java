package com.example.banklab.notification.infrastructure.out.persistence;

import com.example.banklab.events.EventId;
import com.example.banklab.notification.application.port.out.ProcessedEventStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;

public class JdbcProcessedEventStore implements ProcessedEventStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcProcessedEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    @Override
    public boolean tryClaim(EventId eventId) {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        int rowsAffected = jdbcTemplate.update(
                """
                        INSERT INTO processed_event (event_id, processed_at)
                        VALUES (?, CURRENT_TIMESTAMP)
                        ON CONFLICT (event_id) DO NOTHING
                        """,
                eventId.value()
        );
        return rowsAffected == 1;
    }
}
