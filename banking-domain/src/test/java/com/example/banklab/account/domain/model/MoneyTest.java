package com.example.banklab.account.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void should_create_zero_money() {
        assertThat(Money.zero(EUR)).isEqualTo(new Money(BigDecimal.ZERO, EUR));
    }

    @Test
    void should_add_money_with_same_currency() {
        Money result = new Money(new BigDecimal("10.00"), EUR)
                .add(new Money(new BigDecimal("5.50"), EUR));

        assertThat(result).isEqualTo(new Money(new BigDecimal("15.50"), EUR));
    }

    @Test
    void should_subtract_money_with_same_currency() {
        Money result = new Money(new BigDecimal("10.00"), EUR)
                .subtract(new Money(new BigDecimal("4.25"), EUR));

        assertThat(result).isEqualTo(new Money(new BigDecimal("5.75"), EUR));
    }

    @Test
    void should_reject_operations_between_different_currencies() {
        Money euros = new Money(BigDecimal.TEN, EUR);
        Money dollars = new Money(BigDecimal.ONE, USD);

        assertThatThrownBy(() -> euros.add(dollars))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> euros.subtract(dollars))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> euros.isLessThan(dollars))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
