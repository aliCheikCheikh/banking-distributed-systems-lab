package com.example.banklab.account.infrastructure.out.kafka;

import com.example.banklab.account.application.port.out.AccountEventPublisher;
import com.example.banklab.account.domain.event.BankAccountOpened;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaAccountEventPublisher implements AccountEventPublisher {
    private static final String TOPIC = "account-events";
    private final KafkaTemplate<String, BankAccountOpened> kafkaTemplate;

    public KafkaAccountEventPublisher(KafkaTemplate<String, BankAccountOpened> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(BankAccountOpened event) {
        kafkaTemplate.send(TOPIC,
                event.accountId().value().toString(),
                event);

    }
}
