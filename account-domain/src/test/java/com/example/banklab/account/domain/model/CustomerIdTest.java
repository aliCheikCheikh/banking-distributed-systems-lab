package com.example.banklab.account.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CustomerIdTest {

    @Test
    void should_generate_customer_id() {
        assertThat(CustomerId.generate().getValue()).isNotNull();
    }

    @Test
    void should_create_customer_id_from_uuid() {
        UUID value = UUID.randomUUID();

        CustomerId first = CustomerId.of(value);
        CustomerId second = CustomerId.of(value);

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
        assertThat(first.getValue()).isEqualTo(value);
    }

    @Test
    void should_reject_null_value() {
        assertThatNullPointerException().isThrownBy(() -> CustomerId.of(null));
    }
}
