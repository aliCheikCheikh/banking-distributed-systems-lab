package com.example.banklab.account.domain.event;

import com.example.banklab.account.domain.model.AccountId;
import com.example.banklab.account.domain.model.CustomerId;

import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

public record BankAccountOpened(AccountId accountId,
                                CustomerId customerId,
                                Currency currency,
                                Instant occurredAt) {

    public BankAccountOpened {
        Objects.requireNonNull(accountId, "Account ID must not be null");
        Objects.requireNonNull(customerId, "Customer ID must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        Objects.requireNonNull(occurredAt, "Occurred at must not be null");
    }
}
