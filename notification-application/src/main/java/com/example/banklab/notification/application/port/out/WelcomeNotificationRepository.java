package com.example.banklab.notification.application.port.out;

import com.example.banklab.events.account.BankAccountOpened;

public interface WelcomeNotificationRepository {
    void createFor(BankAccountOpened event);
}
