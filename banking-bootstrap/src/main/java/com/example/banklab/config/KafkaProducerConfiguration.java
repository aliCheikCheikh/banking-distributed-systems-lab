package com.example.banklab.config;

import com.example.banklab.account.application.port.in.OpenBankAccountUseCase;
import com.example.banklab.account.application.port.out.AccountEventPublisher;
import com.example.banklab.account.application.service.OpenBankAccountService;
import com.example.banklab.account.domain.event.BankAccountOpened;
import com.example.banklab.account.infrastructure.out.kafka.KafkaAccountEventPublisher;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfiguration {
    @Bean
    public ProducerFactory<String, BankAccountOpened> producerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, BankAccountOpened> kafkaTemplate(ProducerFactory<String, BankAccountOpened> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public AccountEventPublisher accountEventPublisher(KafkaTemplate<String, BankAccountOpened> kafkaTemplate) {
        return new KafkaAccountEventPublisher(kafkaTemplate);
    }

    @Bean
    public OpenBankAccountUseCase openBankAccountUseCase(AccountEventPublisher accountEventPublisher) {
        return new OpenBankAccountService(accountEventPublisher);
    }
}
