package com.example.banklab.account.bootstrap.config;

import com.example.banklab.events.account.BankAccountOpened;
import com.example.banklab.events.EventId;
import com.example.banklab.account.infrastructure.out.kafka.KafkaAccountEventPublisher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                consumerFactory(bootstrapServers);
        Consumer<String, BankAccountOpened> consumer = consumerFactory.createConsumer(
                "account-event-integration-test",
                "test-client"
        );
        BankAccountOpened event = new BankAccountOpened(
                EventId.generate(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "EUR",
                Instant.parse("2026-08-10T10:15:30Z")
        );

        try {
            new KafkaAccountEventPublisher(kafkaTemplate, TOPIC).publish(event);
            kafkaTemplate.flush();

            TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
            consumer.assign(List.of(topicPartition));
            consumer.seekToBeginning(List.of(topicPartition));

            ConsumerRecord<String, BankAccountOpened> record = pollSingleRecord(consumer);

            assertThat(record.topic()).isEqualTo(TOPIC);
            assertThat(record.key()).isEqualTo(event.accountId().toString());
            assertThat(record.value()).isEqualTo(event);
        } finally {
            consumer.close();
            kafkaTemplate.destroy();
        }
    }

    private ConsumerFactory<String, BankAccountOpened> consumerFactory(String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BankAccountOpened.class.getName());
        config.put(JsonDeserializer.TRUSTED_PACKAGES,
                "com.example.banklab.events.account,com.example.banklab.events");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(config);
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
