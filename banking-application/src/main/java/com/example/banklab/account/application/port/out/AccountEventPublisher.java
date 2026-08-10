package com.example.banklab.account.application.port.out;

import com.example.banklab.account.domain.event.BankAccountOpened;

public interface AccountEventPublisher {
    void publish(BankAccountOpened event);
}
