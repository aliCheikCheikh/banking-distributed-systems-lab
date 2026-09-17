package com.example.banklab.account.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "Customer ID must not be null");
    }

    public static CustomerId of(UUID value) {
        return new CustomerId(value);
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerId customerId = (CustomerId) o;
        return Objects.equals(value, customerId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
