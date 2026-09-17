package com.example.banklab.account.infrastructure.in.web;

import com.example.banklab.account.application.dto.OpenBankAccountCommand;
import com.example.banklab.account.application.port.in.OpenBankAccountUseCase;
import com.example.banklab.account.domain.model.BankAccount;
import com.example.banklab.account.domain.model.CustomerId;
import com.example.banklab.account.infrastructure.in.web.dto.OpenBankAccountRequest;
import com.example.banklab.account.infrastructure.in.web.dto.OpenBankAccountResponse;
import com.example.banklab.account.infrastructure.in.web.mapper.BankAccountWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/accounts")
public class OpenBankAccountController {
    private final OpenBankAccountUseCase openBankAccountUseCase;
    private final BankAccountWebMapper bankAccountWebMapper;

    public OpenBankAccountController(OpenBankAccountUseCase openBankAccountUseCase, BankAccountWebMapper bankAccountWebMapper) {
        this.openBankAccountUseCase = Objects.requireNonNull(openBankAccountUseCase, "openBankAccountUseCase must not be null");
        this.bankAccountWebMapper = Objects.requireNonNull(bankAccountWebMapper, "bankAccountWebMapper must not be null");
    }

    @PostMapping
    public ResponseEntity<OpenBankAccountResponse> createAccount(@Valid @RequestBody OpenBankAccountRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        OpenBankAccountCommand command = bankAccountWebMapper.toCommand(request);
        BankAccount account = openBankAccountUseCase.open(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(bankAccountWebMapper.toResponse(account));
    }

}
