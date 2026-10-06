package com.banking.transaction.publisher;

import com.banking.common.event.TransactionEvent;

public interface TransactionEventPublisher {

    void publishTransactionCreated(TransactionEvent event);

    void publishTransactionCompleted(TransactionEvent event);

    void publishTransactionFailed(TransactionEvent event);
}
