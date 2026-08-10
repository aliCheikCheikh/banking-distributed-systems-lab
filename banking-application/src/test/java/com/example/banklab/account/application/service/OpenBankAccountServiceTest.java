package com.example.banklab.account.application.service;

import com.example.banklab.account.application.dto.OpenBankAccountCommand;
import com.example.banklab.account.application.port.in.OpenBankAccountUseCase;
import com.example.banklab.account.application.port.out.AccountEventPublisher;
import com.example.banklab.account.domain.event.BankAccountOpened;
import com.example.banklab.account.domain.model.BankAccount;
import com.example.banklab.account.domain.model.CustomerId;
import com.example.banklab.account.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OpenBankAccountServiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    private AccountEventPublisher eventPublisher;
    private OpenBankAccountUseCase service;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(AccountEventPublisher.class);
        service = new OpenBankAccountService(eventPublisher);
    }

    @Test
    void should_create_bank_account() {
        OpenBankAccountCommand command = command();

        BankAccount account = service.open(command);

        assertThat(account.getCustomerId()).isEqualTo(command.customerId());
        assertThat(account.getCurrency()).isEqualTo(command.currency());
        assertThat(account.getBalance()).isEqualTo(Money.zero(EUR));
    }

    @Test
    void should_publish_exactly_one_bank_account_opened_event() {
        service.open(command());

        verify(eventPublisher).publish(eventCaptor().capture());
    }

    @Test
    void should_publish_event_matching_created_account() {
        BankAccount account = service.open(command());
        ArgumentCaptor<BankAccountOpened> captor = eventCaptor();

        verify(eventPublisher).publish(captor.capture());
        BankAccountOpened publishedEvent = captor.getValue();

        assertThat(publishedEvent.accountId()).isEqualTo(account.getAccountId());
        assertThat(publishedEvent.customerId()).isEqualTo(account.getCustomerId());
        assertThat(publishedEvent.currency()).isEqualTo(account.getCurrency());
        assertThat(publishedEvent.occurredAt()).isEqualTo(account.getOpenedAt());
    }

    @Test
    void should_reject_null_command_without_publishing_event() {
        assertThatNullPointerException()
                .isThrownBy(() -> service.open(null))
                .withMessage("command cannot be null");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_not_publish_event_when_customer_is_missing() {
        OpenBankAccountCommand command = new OpenBankAccountCommand(null, EUR);

        assertThatNullPointerException()
                .isThrownBy(() -> service.open(command))
                .withMessage("Customer ID must not be null");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_not_publish_event_when_currency_is_missing() {
        OpenBankAccountCommand command = new OpenBankAccountCommand(CustomerId.generate(), null);

        assertThatNullPointerException()
                .isThrownBy(() -> service.open(command))
                .withMessage("Currency must not be null");

        verifyNoInteractions(eventPublisher);
    }

    private OpenBankAccountCommand command() {
        return new OpenBankAccountCommand(CustomerId.of(UUID.randomUUID()), EUR);
    }

    private ArgumentCaptor<BankAccountOpened> eventCaptor() {
        return ArgumentCaptor.forClass(BankAccountOpened.class);
    }
}
