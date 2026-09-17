package com.example.banklab.events;

import java.util.Objects;
import java.util.UUID;

public record EventId(UUID value) {
    public EventId {
        Objects.requireNonNull(value, "Event ID must not be null");
    }

    public static EventId generate() {
        return new EventId(UUID.randomUUID());
    }

    public static EventId of(UUID eventId) {
        return new EventId(eventId);
    }

}
