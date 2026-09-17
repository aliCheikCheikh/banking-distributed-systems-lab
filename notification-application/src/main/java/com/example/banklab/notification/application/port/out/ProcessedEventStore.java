package com.example.banklab.notification.application.port.out;

import com.example.banklab.events.EventId;

public interface ProcessedEventStore {
    boolean tryClaim(EventId eventId);
}
