package com.example.banklab.events.account;

import com.example.banklab.events.EventId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BankAccountOpened(EventId eventId,
                                UUID accountId,
                                UUID customerId,
                                String currency,
                                Instant occurredAt) {

    public BankAccountOpened {
        Objects.requireNonNull(eventId, "Event ID must not be null");
        Objects.requireNonNull(accountId, "Account ID must not be null");
        Objects.requireNonNull(customerId, "Customer ID must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        Objects.requireNonNull(occurredAt, "Occurred at must not be null");
    }
}
