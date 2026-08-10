package com.example.banklab.account.infrastructure.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Currency;
import java.util.UUID;

public record OpenBankAccountRequest(@NotNull UUID customerId,
                                     @NotNull Currency currency) {
}
