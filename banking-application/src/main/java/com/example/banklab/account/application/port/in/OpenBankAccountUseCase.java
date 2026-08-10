package com.example.banklab.account.application.port.in;

import com.example.banklab.account.application.dto.OpenBankAccountCommand;
import com.example.banklab.account.domain.model.BankAccount;

public interface OpenBankAccountUseCase {
    BankAccount open(OpenBankAccountCommand command);
}
