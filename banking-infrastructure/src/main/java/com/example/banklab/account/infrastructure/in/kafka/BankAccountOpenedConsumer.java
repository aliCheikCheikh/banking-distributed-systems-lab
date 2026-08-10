package com.example.banklab.account.infrastructure.in.kafka;

import com.example.banklab.account.domain.event.BankAccountOpened;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BankAccountOpenedConsumer {
    @KafkaListener(
            topics = "account-events",
            groupId = "notification-service"
    )
    public void consume(BankAccountOpened event) {
        System.out.println(

                ">>> ATTEMPT - accountId=" + event.accountId()

                        + " - time=" + java.time.Instant.now()

        );
    }
}
