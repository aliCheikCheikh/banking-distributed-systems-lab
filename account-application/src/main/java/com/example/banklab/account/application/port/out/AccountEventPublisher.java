package com.example.banklab.account.application.port.out;

import com.example.banklab.events.account.BankAccountOpened;

public interface AccountEventPublisher {
    void publish(BankAccountOpened event);
}
