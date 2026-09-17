package com.example.banklab.notification.infrastructure.in.kafka;

import com.example.banklab.events.account.BankAccountOpened;
import com.example.banklab.notification.application.port.in.ProcessBankAccountOpenedUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class BankAccountOpenedConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BankAccountOpenedConsumer.class);

    private final ProcessBankAccountOpenedUseCase processBankAccountOpenedUseCase;

    public BankAccountOpenedConsumer(ProcessBankAccountOpenedUseCase processBankAccountOpenedUseCase) {
        this.processBankAccountOpenedUseCase = Objects.requireNonNull(processBankAccountOpenedUseCase);
    }

    @KafkaListener(
            topics = "${notification.kafka.topic:account-events}",
            groupId = "${notification.kafka.consumer.group-id:notification-service}"
    )
    public void consume(BankAccountOpened event) {
        LOGGER.info("BankAccountOpened received eventId={} accountId={}",
                event.eventId().value(),
                event.accountId());
        processBankAccountOpenedUseCase.process(event);
    }
}
