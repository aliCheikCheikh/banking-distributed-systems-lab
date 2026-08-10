package com.example.banklab.account.infrastructure.in.web.mapper;

import com.example.banklab.account.application.dto.OpenBankAccountCommand;
import com.example.banklab.account.domain.model.BankAccount;
import com.example.banklab.account.domain.model.CustomerId;
import com.example.banklab.account.infrastructure.in.web.dto.OpenBankAccountRequest;
import com.example.banklab.account.infrastructure.in.web.dto.OpenBankAccountResponse;
import org.springframework.stereotype.Component;

@Component
public class BankAccountWebMapper {
    public OpenBankAccountCommand toCommand(OpenBankAccountRequest request) {
        return new OpenBankAccountCommand(CustomerId.of(request.customerId()), request.currency());
    }

    public OpenBankAccountResponse toResponse(BankAccount bankAccount) {
        return new OpenBankAccountResponse(bankAccount.getAccountId().getValue(),
                bankAccount.getCustomerId().getValue(),
                bankAccount.getBalance().amount(),
                bankAccount.getCurrency(),
                bankAccount.getOpenedAt());
    }
}
