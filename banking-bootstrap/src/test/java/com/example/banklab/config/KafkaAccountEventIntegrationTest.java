package com.example.banklab.config;

import com.example.banklab.account.domain.event.BankAccountOpened;
import com.example.banklab.account.domain.model.AccountId;
import com.example.banklab.account.domain.model.CustomerId;
import com.example.banklab.account.infrastructure.out.kafka.KafkaAccountEventPublisher;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KafkaAccountEventIntegrationTest {

    private static final String TOPIC = "account-events";

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.9.1")
    );

    @Test
    void should_publish_and_deserialize_bank_account_opened_event() {
        String bootstrapServers = KAFKA.getBootstrapServers();
        ProducerFactory<String, BankAccountOpened> producerFactory =
                new KafkaProducerConfiguration().producerFactory(bootstrapServers);
        KafkaTemplate<String, BankAccountOpened> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        ConsumerFactory<String, BankAccountOpened> consumerFactory =
                new KafkaConsumerConfiguration().consumerFactory(bootstrapServers);
        Consumer<String, BankAccountOpened> consumer = consumerFactory.createConsumer(
                "account-event-integration-test",
                "test-client"
        );
        BankAccountOpened event = new BankAccountOpened(
                AccountId.generate(),
                CustomerId.generate(),
                Currency.getInstance("EUR"),
                Instant.parse("2026-08-10T10:15:30Z")
        );

        try {
            new KafkaAccountEventPublisher(kafkaTemplate).publish(event);
            kafkaTemplate.flush();

            TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
            consumer.assign(List.of(topicPartition));
            consumer.seekToBeginning(List.of(topicPartition));

            ConsumerRecord<String, BankAccountOpened> record = pollSingleRecord(consumer);

            assertThat(record.topic()).isEqualTo(TOPIC);
            assertThat(record.key()).isEqualTo(event.accountId().value().toString());
            assertThat(record.value()).isEqualTo(event);
        } finally {
            consumer.close();
            kafkaTemplate.destroy();
        }
    }

    private ConsumerRecord<String, BankAccountOpened> pollSingleRecord(
            Consumer<String, BankAccountOpened> consumer) {
        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, BankAccountOpened> records = consumer.poll(Duration.ofMillis(250));
            if (!records.isEmpty()) {
                assertThat(records.count()).isOne();
                return records.iterator().next();
            }
        }
        throw new AssertionError("No BankAccountOpened event received from Kafka within 10 seconds");
    }
}
