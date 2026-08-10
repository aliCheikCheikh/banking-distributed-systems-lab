package com.example.banklab.account.infrastructure.in.web;

import com.example.banklab.account.application.dto.OpenBankAccountCommand;
import com.example.banklab.account.application.port.in.OpenBankAccountUseCase;
import com.example.banklab.account.domain.model.BankAccount;
import com.example.banklab.account.domain.model.CustomerId;
import com.example.banklab.account.infrastructure.in.web.mapper.BankAccountWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class OpenBankAccountControllerTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    private OpenBankAccountUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = mock(OpenBankAccountUseCase.class);
        OpenBankAccountController controller = new OpenBankAccountController(useCase, new BankAccountWebMapper());
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void should_open_account_and_return_created_response() throws Exception {
        UUID customerId = UUID.randomUUID();
        BankAccount account = BankAccount.open(CustomerId.of(customerId), EUR);
        when(useCase.open(any(OpenBankAccountCommand.class))).thenReturn(account);

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "currency": "EUR"
                                }
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(account.getAccountId().getValue().toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.openedAt").exists());

        ArgumentCaptor<OpenBankAccountCommand> commandCaptor = ArgumentCaptor.forClass(OpenBankAccountCommand.class);
        verify(useCase).open(commandCaptor.capture());
        assertThat(commandCaptor.getValue().customerId()).isEqualTo(CustomerId.of(customerId));
        assertThat(commandCaptor.getValue().currency()).isEqualTo(EUR);
    }

    @Test
    void should_reject_request_without_customer_id() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency": "EUR"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_reject_request_without_currency() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_reject_malformed_json() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json}"))
                .andExpect(status().isBadRequest());
    }
}
