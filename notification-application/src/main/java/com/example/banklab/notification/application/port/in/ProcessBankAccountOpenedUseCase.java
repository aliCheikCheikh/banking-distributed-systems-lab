package com.example.banklab.notification.application.port.in;

import com.example.banklab.events.account.BankAccountOpened;

public interface ProcessBankAccountOpenedUseCase {
    void process(BankAccountOpened event);
}
