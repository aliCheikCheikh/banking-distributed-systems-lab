package com.example.banklab.notification.bootstrap.config;

import com.example.banklab.notification.application.port.in.ProcessBankAccountOpenedUseCase;
import com.example.banklab.notification.application.port.out.ProcessedEventStore;
import com.example.banklab.notification.application.port.out.TransactionRunner;
import com.example.banklab.notification.application.port.out.WelcomeNotificationRepository;
import com.example.banklab.notification.application.service.IdempotentBankAccountOpenedService;
import com.example.banklab.notification.infrastructure.out.persistence.JdbcProcessedEventStore;
import com.example.banklab.notification.infrastructure.out.persistence.JdbcWelcomeNotificationRepository;
import com.example.banklab.notification.infrastructure.transaction.SpringTransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class NotificationPersistenceConfiguration {

    @Bean
    public ProcessedEventStore processedEventStore(JdbcTemplate jdbcTemplate) {
        return new JdbcProcessedEventStore(jdbcTemplate);
    }

    @Bean
    public WelcomeNotificationRepository welcomeNotificationRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcWelcomeNotificationRepository(jdbcTemplate);
    }

    @Bean
    public TransactionRunner transactionRunner(PlatformTransactionManager transactionManager) {
        return new SpringTransactionRunner(new TransactionTemplate(transactionManager));
    }

    @Bean
    public ProcessBankAccountOpenedUseCase processBankAccountOpenedUseCase(
            ProcessedEventStore processedEventStore,
            WelcomeNotificationRepository welcomeNotificationRepository,
            TransactionRunner transactionRunner) {
        return new IdempotentBankAccountOpenedService(
                processedEventStore,
                welcomeNotificationRepository,
                transactionRunner
        );
    }
}
