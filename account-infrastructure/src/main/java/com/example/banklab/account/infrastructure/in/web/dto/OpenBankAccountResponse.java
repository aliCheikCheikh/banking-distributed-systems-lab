package com.example.banklab.account.infrastructure.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

public record OpenBankAccountResponse(UUID accountId,
                                      UUID customerId,
                                      BigDecimal balance,
                                      Currency currency,
                                      Instant openedAt) {
}
