package com.example.banklab.notification.bootstrap.config;

import com.example.banklab.events.account.BankAccountOpened;
import com.example.banklab.events.EventId;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KafkaRetryDltIntegrationTest {

    private static final String TOPIC = "account-events";
    private static final String DLT_TOPIC = TOPIC + "-dlt";

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.9.1")
    );

    @Test
    void should_retry_twice_then_publish_non_recoverable_record_to_dlt() {
        String bootstrapServers = KAFKA.getBootstrapServers();
        KafkaConsumerConfiguration producerConfiguration = new KafkaConsumerConfiguration();
        ProducerFactory<String, BankAccountOpened> producerFactory =
                producerConfiguration.producerFactory(bootstrapServers);
        KafkaTemplate<String, BankAccountOpened> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        KafkaConsumerConfiguration consumerConfiguration = new KafkaConsumerConfiguration();
        ConsumerFactory<String, BankAccountOpened> consumerFactory =
                consumerConfiguration.consumerFactory(bootstrapServers);
        AtomicInteger attempts = new AtomicInteger();
        ContainerProperties properties = new ContainerProperties(TOPIC);
        properties.setGroupId("retry-dlt-integration-test");
        properties.setMessageListener((MessageListener<String, BankAccountOpened>) record -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("non-recoverable test failure");
        });
        ConcurrentMessageListenerContainer<String, BankAccountOpened> listener =
                new ConcurrentMessageListenerContainer<>(consumerFactory, properties);
        listener.setCommonErrorHandler(consumerConfiguration.errorHandler(
                consumerConfiguration.deadLetterPublishingRecoverer(kafkaTemplate)
        ));
        Consumer<String, BankAccountOpened> dltConsumer = consumerFactory.createConsumer(
                "dlt-verification-test",
                "dlt-client"
        );
        BankAccountOpened event = event();

        try {
            listener.start();
            waitForAssignment(listener);
            kafkaTemplate.send(TOPIC, event.accountId().toString(), event);
            kafkaTemplate.flush();

            TopicPartition dltPartition = new TopicPartition(DLT_TOPIC, 0);
            dltConsumer.assign(List.of(dltPartition));
            dltConsumer.seekToBeginning(List.of(dltPartition));
            ConsumerRecord<String, BankAccountOpened> deadLetter = pollSingleRecord(dltConsumer);

            assertThat(attempts).hasValue(3);
            assertThat(deadLetter.topic()).isEqualTo(DLT_TOPIC);
            assertThat(deadLetter.key()).isEqualTo(event.accountId().toString());
            assertThat(deadLetter.value()).isEqualTo(event);
        } finally {
            listener.stop();
            dltConsumer.close();
            kafkaTemplate.destroy();
        }
    }

    private void waitForAssignment(
            ConcurrentMessageListenerContainer<String, BankAccountOpened> listener) {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            if (listener.getAssignedPartitions().size() == 1) {
                return;
            }
            LockSupport.parkNanos(Duration.ofMillis(50).toNanos());
        }
        throw new AssertionError("Kafka listener received no partition assignment within 15 seconds");
    }

    private ConsumerRecord<String, BankAccountOpened> pollSingleRecord(
            Consumer<String, BankAccountOpened> consumer) {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, BankAccountOpened> records = consumer.poll(Duration.ofMillis(250));
            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }
        throw new AssertionError("No record received from DLT within 15 seconds");
    }

    private BankAccountOpened event() {
        return new BankAccountOpened(
                EventId.generate(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "EUR",
                Instant.parse("2026-08-13T10:15:30Z")
        );
    }
}
