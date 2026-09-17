package com.example.banklab.notification.bootstrap.dev;

import com.example.banklab.events.account.BankAccountOpened;
import com.example.banklab.events.EventId;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class BankAccountOpenedRaceReplayRunnerTest {

    @Test
    @SuppressWarnings("unchecked")
    void should_replay_same_event_to_partitions_zero_and_one() {
        KafkaTemplate<String, BankAccountOpened> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(anyString(), anyInt(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        String eventId = "6e840c4e-823c-4e52-9674-6f16cf3b637b";
        String accountId = "97981ddf-23fe-4449-9a8a-3debaa01cb1f";
        String customerId = "17711250-4fb6-480d-9fef-fae856ccc71d";
        BankAccountOpened expectedEvent = new BankAccountOpened(
                EventId.of(UUID.fromString(eventId)),
                UUID.fromString(accountId),
                UUID.fromString(customerId),
                "EUR",
                Instant.parse("2026-08-15T10:15:30Z")
        );
        BankAccountOpenedRaceReplayRunner runner = new BankAccountOpenedRaceReplayRunner(
                kafkaTemplate,
                eventId,
                accountId,
                customerId,
                "EUR",
                "2026-08-15T10:15:30Z",
                "account-events"
        );

        runner.run();

        var ordered = inOrder(kafkaTemplate);
        ordered.verify(kafkaTemplate).send("account-events", 0, accountId, expectedEvent);
        ordered.verify(kafkaTemplate).send("account-events", 1, accountId, expectedEvent);
        verifyNoMoreInteractions(kafkaTemplate);
    }
}
