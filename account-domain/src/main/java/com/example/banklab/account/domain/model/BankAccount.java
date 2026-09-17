package com.example.banklab.account.domain.model;

import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

public class BankAccount {
    private final AccountId accountId;
    private final CustomerId customerId;
    private Money balance;
    private final Instant openedAt;

    private BankAccount(AccountId accountId, CustomerId customerId, Money balance, Instant openedAt) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.balance = balance;
        this.openedAt = openedAt;
    }

    public static BankAccount open(CustomerId customerId, Currency currency) {
        Objects.requireNonNull(customerId, "Customer ID must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        return new BankAccount(AccountId.generate(), customerId, Money.zero(currency), Instant.now());
    }

    public void deposit(Money amount) {
        Objects.requireNonNull(amount, "Amount must not be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        balance = balance.add(amount);
    }

    public void withdraw(Money amount) {
        Objects.requireNonNull(amount, "Amount must not be null");

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (balance.isLessThan(amount)) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        balance = balance.subtract(amount);
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public Money getBalance() {
        return balance;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public Currency getCurrency() {
        return balance.currency();
    }

    public Instant getOpenedAt() {
        return openedAt;
    }
}
