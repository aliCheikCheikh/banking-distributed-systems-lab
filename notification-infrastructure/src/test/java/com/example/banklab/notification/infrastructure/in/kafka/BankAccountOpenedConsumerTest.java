package com.example.banklab.notification.infrastructure.in.kafka;

import com.example.banklab.events.account.BankAccountOpened;
import com.example.banklab.events.EventId;
import com.example.banklab.notification.application.port.in.ProcessBankAccountOpenedUseCase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BankAccountOpenedConsumerTest {

    @Test
    void should_delegate_consumed_event_to_use_case() {
        ProcessBankAccountOpenedUseCase useCase = mock(ProcessBankAccountOpenedUseCase.class);
        BankAccountOpenedConsumer consumer = new BankAccountOpenedConsumer(useCase);
        BankAccountOpened event = new BankAccountOpened(
                EventId.generate(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "EUR",
                Instant.parse("2026-08-13T10:15:30Z")
        );

        consumer.consume(event);

        verify(useCase).process(event);
    }
}
