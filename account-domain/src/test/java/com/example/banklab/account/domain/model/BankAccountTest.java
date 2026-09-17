package com.example.banklab.account.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankAccountTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    private CustomerId customerId;

    @BeforeEach
    void setUp() {
        customerId = CustomerId.of(UUID.randomUUID());
    }

    @Test
    void should_open_bank_account_with_zero_balance() {
        BankAccount account = BankAccount.open(customerId, EUR);

        assertThat(account.getCustomerId()).isEqualTo(customerId);
        assertThat(account.getCurrency()).isEqualTo(EUR);
        assertThat(account.getBalance()).isEqualTo(Money.zero(EUR));
        assertThat(account.getAccountId()).isNotNull();
        assertThat(account.getOpenedAt()).isNotNull();
    }

    @Test
    void should_reject_opening_account_without_customer() {
        assertThatNullPointerException()
                .isThrownBy(() -> BankAccount.open(null, EUR))
                .withMessage("Customer ID must not be null");
    }

    @Test
    void should_reject_opening_account_without_currency() {
        assertThatNullPointerException()
                .isThrownBy(() -> BankAccount.open(customerId, null))
                .withMessage("Currency must not be null");
    }

    @Test
    void should_deposit_positive_money() {
        BankAccount account = BankAccount.open(customerId, EUR);

        account.deposit(euros("150.00"));

        assertThat(account.getBalance()).isEqualTo(euros("150.00"));
    }

    @Test
    void should_reject_zero_deposit() {
        BankAccount account = BankAccount.open(customerId, EUR);

        assertThatThrownBy(() -> account.deposit(Money.zero(EUR)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_negative_deposit() {
        BankAccount account = BankAccount.open(customerId, EUR);

        assertThatThrownBy(() -> account.deposit(euros("-150.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_deposit_without_amount() {
        BankAccount account = BankAccount.open(customerId, EUR);

        assertThatNullPointerException()
                .isThrownBy(() -> account.deposit(null))
                .withMessage("Amount must not be null");
    }

    @Test
    void should_reject_deposit_in_another_currency() {
        BankAccount account = BankAccount.open(customerId, EUR);
        Money dollars = new Money(BigDecimal.TEN, Currency.getInstance("USD"));

        assertThatThrownBy(() -> account.deposit(dollars))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currencies must match");
    }

    @Test
    void should_withdraw_positive_money() {
        BankAccount account = accountWithBalance("150.00");

        account.withdraw(euros("50.00"));

        assertThat(account.getBalance()).isEqualTo(euros("100.00"));
    }

    @Test
    void should_reject_zero_withdrawal() {
        BankAccount account = accountWithBalance("150.00");

        assertThatThrownBy(() -> account.withdraw(Money.zero(EUR)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_negative_withdrawal() {
        BankAccount account = accountWithBalance("150.00");

        assertThatThrownBy(() -> account.withdraw(euros("-50.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_withdrawal_without_amount() {
        BankAccount account = accountWithBalance("150.00");

        assertThatNullPointerException()
                .isThrownBy(() -> account.withdraw(null))
                .withMessage("Amount must not be null");
    }

    @Test
    void should_reject_withdrawal_in_another_currency() {
        BankAccount account = accountWithBalance("150.00");
        Money dollars = new Money(BigDecimal.TEN, Currency.getInstance("USD"));

        assertThatThrownBy(() -> account.withdraw(dollars))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currencies must match");
    }

    @Test
    void should_reject_withdrawal_greater_than_balance() {
        BankAccount account = accountWithBalance("150.00");

        assertThatThrownBy(() -> account.withdraw(euros("150.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_allow_withdrawal_equal_to_balance() {
        BankAccount account = accountWithBalance("150.00");

        account.withdraw(euros("150.00"));

        assertThat(account.getBalance()).isEqualTo(Money.zero(EUR));
    }

    private BankAccount accountWithBalance(String amount) {
        BankAccount account = BankAccount.open(customerId, EUR);
        account.deposit(euros(amount));
        return account;
    }

    private Money euros(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
