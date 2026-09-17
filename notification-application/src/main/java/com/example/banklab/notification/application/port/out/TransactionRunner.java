package com.example.banklab.notification.application.port.out;

public interface TransactionRunner {
    void run(Runnable action);
}
