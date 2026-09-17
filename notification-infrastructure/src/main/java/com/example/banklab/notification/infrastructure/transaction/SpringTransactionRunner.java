package com.example.banklab.notification.infrastructure.transaction;

import com.example.banklab.notification.application.port.out.TransactionRunner;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

public class SpringTransactionRunner implements TransactionRunner {
    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = Objects.requireNonNull(
                transactionTemplate,
                "transactionTemplate cannot be null"
        );
    }

    @Override
    public void run(Runnable action) {
        Objects.requireNonNull(action, "action cannot be null");
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}
