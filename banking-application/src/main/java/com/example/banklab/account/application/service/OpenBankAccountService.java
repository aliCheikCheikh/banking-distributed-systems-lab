package com.example.banklab.account.application.service;

import com.example.banklab.account.application.dto.OpenBankAccountCommand;
import com.example.banklab.account.application.port.in.OpenBankAccountUseCase;
import com.example.banklab.account.application.port.out.AccountEventPublisher;
import com.example.banklab.account.domain.event.BankAccountOpened;
import com.example.banklab.account.domain.model.BankAccount;

import java.util.Objects;

public class OpenBankAccountService implements OpenBankAccountUseCase {
    private final AccountEventPublisher eventPublisher;

    public OpenBankAccountService(AccountEventPublisher eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public BankAccount open(OpenBankAccountCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        BankAccount account = BankAccount.open(command.customerId(), command.currency());
        BankAccountOpened event = new BankAccountOpened(account.getAccountId(),
                account.getCustomerId(),
                account.getCurrency(),
                account.getOpenedAt());
        eventPublisher.publish(event);
        return account;
    }
}
