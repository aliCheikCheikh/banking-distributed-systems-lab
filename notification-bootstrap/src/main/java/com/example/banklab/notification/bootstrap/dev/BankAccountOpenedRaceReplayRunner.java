package com.example.banklab.notification.bootstrap.dev;

import com.example.banklab.events.account.BankAccountOpened;
import com.example.banklab.events.EventId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

@Component
@Profile("dev-race")
@ConditionalOnProperty(prefix = "dev-race", name = "enabled", havingValue = "true")
public class BankAccountOpenedRaceReplayRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BankAccountOpenedRaceReplayRunner.class);
    private final KafkaTemplate<String, BankAccountOpened> kafkaTemplate;
    private final BankAccountOpened event;
    private final String topic;

    public BankAccountOpenedRaceReplayRunner(
            KafkaTemplate<String, BankAccountOpened> kafkaTemplate,
            @Value("${dev-race.event-id}") String eventId,
            @Value("${dev-race.account-id}") String accountId,
            @Value("${dev-race.customer-id}") String customerId,
            @Value("${dev-race.currency}") String currency,
            @Value("${dev-race.occurred-at}") String occurredAt,
            @Value("${notification.kafka.topic:account-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.event = new BankAccountOpened(
                EventId.of(UUID.fromString(eventId)),
                UUID.fromString(accountId),
                UUID.fromString(customerId),
                Currency.getInstance(currency).getCurrencyCode(),
                Instant.parse(occurredAt)
        );
    }

    @Override
    public void run(String... args) {
        String key = event.accountId().toString();

        var partitionZeroSend = kafkaTemplate.send(topic, 0, key, event);
        var partitionOneSend = kafkaTemplate.send(topic, 1, key, event);

        partitionZeroSend.thenCombine(partitionOneSend, (first, second) -> null).join();

        LOGGER.info("Replayed eventId={} to {} partitions 0 and 1",
                event.eventId().value(), topic);
    }
}
