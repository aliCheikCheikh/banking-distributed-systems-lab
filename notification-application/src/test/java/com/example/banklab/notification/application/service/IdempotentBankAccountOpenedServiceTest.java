package com.example.banklab.notification.application.service;

import com.example.banklab.events.account.BankAccountOpened;
import com.example.banklab.events.EventId;
import com.example.banklab.notification.application.port.out.ProcessedEventStore;
import com.example.banklab.notification.application.port.out.TransactionRunner;
import com.example.banklab.notification.application.port.out.WelcomeNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotentBankAccountOpenedServiceTest {
    private ProcessedEventStore processedEventStore;
    private WelcomeNotificationRepository welcomeNotificationRepository;
    private IdempotentBankAccountOpenedService service;
    private TransactionRunner transactionRunner;

    @BeforeEach
    void setUp() {
        processedEventStore = mock(ProcessedEventStore.class);
        welcomeNotificationRepository = mock(WelcomeNotificationRepository.class);
        transactionRunner = mock(TransactionRunner.class);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(transactionRunner).run(any(Runnable.class));
        service = new IdempotentBankAccountOpenedService(
                processedEventStore,
                welcomeNotificationRepository,
                transactionRunner
        );
    }

    @Test
    void should_not_create_notification_when_event_cannot_be_claimed() {
        BankAccountOpened event = event();
        when(processedEventStore.tryClaim(event.eventId())).thenReturn(false);

        service.process(event);

        verify(processedEventStore).tryClaim(event.eventId());
        verify(welcomeNotificationRepository, never()).createFor(event);
        verify(transactionRunner).run(any(Runnable.class));
    }

    @Test
    void should_create_notification_when_event_is_successfully_claimed() {
        BankAccountOpened event = event();
        when(processedEventStore.tryClaim(event.eventId())).thenReturn(true);

        service.process(event);

        verify(processedEventStore).tryClaim(event.eventId());
        verify(welcomeNotificationRepository).createFor(event);
        verify(transactionRunner).run(any(Runnable.class));
    }

    @Test
    void same_event_replayed_should_create_welcome_notification_only_once() {
        BankAccountOpened event = event();
        when(processedEventStore.tryClaim(event.eventId())).thenReturn(true, false);

        service.process(event);
        service.process(event);

        verify(processedEventStore, times(2)).tryClaim(event.eventId());
        verify(welcomeNotificationRepository).createFor(event);
        verify(transactionRunner, times(2)).run(any(Runnable.class));
    }

    @Test
    void different_events_should_be_processed_independently() {
        BankAccountOpened firstEvent = event();
        BankAccountOpened secondEvent = event();
        when(processedEventStore.tryClaim(firstEvent.eventId())).thenReturn(true);
        when(processedEventStore.tryClaim(secondEvent.eventId())).thenReturn(true);

        service.process(firstEvent);
        service.process(secondEvent);

        verify(welcomeNotificationRepository).createFor(firstEvent);
        verify(welcomeNotificationRepository).createFor(secondEvent);
        verify(processedEventStore).tryClaim(firstEvent.eventId());
        verify(processedEventStore).tryClaim(secondEvent.eventId());
        verify(transactionRunner, times(2)).run(any(Runnable.class));
    }

    @Test
    void notification_persistence_failure_should_be_propagated_from_transaction() {
        BankAccountOpened event = event();
        when(processedEventStore.tryClaim(event.eventId())).thenReturn(true);
        doThrow(new IllegalStateException("notification persistence failed"))
                .when(welcomeNotificationRepository)
                .createFor(event);

        assertThatThrownBy(() -> service.process(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("notification persistence failed");

        verify(processedEventStore).tryClaim(event.eventId());
        verify(transactionRunner).run(any(Runnable.class));
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
