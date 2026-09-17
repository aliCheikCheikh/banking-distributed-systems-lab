package com.example.banklab.account.application.dto;

import com.example.banklab.account.domain.model.CustomerId;

import java.util.Currency;

public record OpenBankAccountCommand(CustomerId customerId, Currency currency) {
}
