package com.example.banklab.notification.application.service;

import com.example.banklab.events.account.BankAccountOpened;
import com.example.banklab.notification.application.port.in.ProcessBankAccountOpenedUseCase;
import com.example.banklab.notification.application.port.out.ProcessedEventStore;
import com.example.banklab.notification.application.port.out.TransactionRunner;
import com.example.banklab.notification.application.port.out.WelcomeNotificationRepository;
import java.util.Objects;

public class IdempotentBankAccountOpenedService implements ProcessBankAccountOpenedUseCase {
    private final ProcessedEventStore processedEventStore;
    private final WelcomeNotificationRepository welcomeNotificationRepository;
    private final TransactionRunner transactionRunner;

    public IdempotentBankAccountOpenedService(ProcessedEventStore processedEventStore,
                                              WelcomeNotificationRepository welcomeNotificationRepository,
                                              TransactionRunner transactionRunner) {
        this.processedEventStore = Objects.requireNonNull(processedEventStore);
        this.welcomeNotificationRepository = Objects.requireNonNull(welcomeNotificationRepository);
        this.transactionRunner = Objects.requireNonNull(transactionRunner);
    }

    @Override
    public void process(BankAccountOpened event) {
        Objects.requireNonNull(event, "event cannot be null");
        transactionRunner.run(() -> {
            if (processedEventStore.tryClaim(event.eventId())) {
                welcomeNotificationRepository.createFor(event);
            }
        });
    }
}
