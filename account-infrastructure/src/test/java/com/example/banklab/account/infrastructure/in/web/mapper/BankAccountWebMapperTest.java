package com.example.banklab.account.infrastructure.in.web.mapper;

import com.example.banklab.account.application.dto.OpenBankAccountCommand;
import com.example.banklab.account.domain.model.BankAccount;
import com.example.banklab.account.domain.model.CustomerId;
import com.example.banklab.account.infrastructure.in.web.dto.OpenBankAccountRequest;
import com.example.banklab.account.infrastructure.in.web.dto.OpenBankAccountResponse;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BankAccountWebMapperTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private final BankAccountWebMapper mapper = new BankAccountWebMapper();

    @Test
    void should_map_request_to_command() {
        UUID customerId = UUID.randomUUID();
        OpenBankAccountRequest request = new OpenBankAccountRequest(customerId, EUR);

        OpenBankAccountCommand command = mapper.toCommand(request);

        assertThat(command.customerId()).isEqualTo(CustomerId.of(customerId));
        assertThat(command.currency()).isEqualTo(EUR);
    }

    @Test
    void should_map_bank_account_to_response() {
        BankAccount account = BankAccount.open(CustomerId.generate(), EUR);

        OpenBankAccountResponse response = mapper.toResponse(account);

        assertThat(response.accountId()).isEqualTo(account.getAccountId().value());
        assertThat(response.customerId()).isEqualTo(account.getCustomerId().value());
        assertThat(response.balance()).isEqualByComparingTo(account.getBalance().amount());
        assertThat(response.currency()).isEqualTo(account.getCurrency());
        assertThat(response.openedAt()).isEqualTo(account.getOpenedAt());
    }
}
