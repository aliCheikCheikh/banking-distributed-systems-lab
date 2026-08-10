package com.example.banklab.account.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AccountIdTest {

    @Test
    void should_generate_account_id() {
        assertThat(AccountId.generate().getValue()).isNotNull();
    }

    @Test
    void should_create_account_id_from_uuid() {
        UUID value = UUID.randomUUID();

        AccountId first = AccountId.of(value);
        AccountId second = AccountId.of(value);

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
        assertThat(first.getValue()).isEqualTo(value);
    }

    @Test
    void should_reject_null_value() {
        assertThatNullPointerException().isThrownBy(() -> AccountId.of(null));
    }
}
