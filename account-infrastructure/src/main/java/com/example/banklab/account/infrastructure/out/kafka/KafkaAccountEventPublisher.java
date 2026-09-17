package com.example.banklab.account.infrastructure.out.kafka;

import com.example.banklab.account.application.port.out.AccountEventPublisher;
import com.example.banklab.events.account.BankAccountOpened;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Objects;

public class KafkaAccountEventPublisher implements AccountEventPublisher {
    private final KafkaTemplate<String, BankAccountOpened> kafkaTemplate;
    private final String topic;

    public KafkaAccountEventPublisher(
            KafkaTemplate<String, BankAccountOpened> kafkaTemplate,
            String topic
    ) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate);
        this.topic = Objects.requireNonNull(topic);
    }

    @Override
    public void publish(BankAccountOpened event) {
        kafkaTemplate.send(topic,
                event.accountId().toString(),
                event);
    }
}
