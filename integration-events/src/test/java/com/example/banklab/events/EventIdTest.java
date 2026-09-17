package com.example.banklab.events;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventIdTest {

    @Test
    void should_generate_event_identity() {
        EventId eventId = EventId.generate();

        assertThat(eventId.value()).isNotNull();
    }

    @Test
    void should_reconstruct_event_identity_from_uuid() {
        UUID value = UUID.randomUUID();

        assertThat(EventId.of(value)).isEqualTo(new EventId(value));
    }
}
