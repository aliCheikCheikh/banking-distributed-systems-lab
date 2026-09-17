package com.example.banklab.account.infrastructure.out.kafka;

import com.example.banklab.events.account.BankAccountOpened;
import com.example.banklab.events.EventId;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaAccountEventPublisherTest {

    @Test
    @SuppressWarnings("unchecked")
    void should_publish_event_to_account_events_using_account_id_as_key() {
        KafkaTemplate<String, BankAccountOpened> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaAccountEventPublisher publisher = new KafkaAccountEventPublisher(kafkaTemplate, "account-events");
        BankAccountOpened event = new BankAccountOpened(
                EventId.generate(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "EUR",
                Instant.parse("2026-08-10T10:15:30Z")
        );

        publisher.publish(event);

        verify(kafkaTemplate).send("account-events", event.accountId().toString(), event);
    }
}
