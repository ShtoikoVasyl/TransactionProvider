package edu.shtoiko.transactionprowider.provider.transactionstack;

import edu.shtoiko.transactionprowider.model.entity.Transaction;
import reactor.core.publisher.Flux;

public interface TransactionStackProvider {
    Flux<Transaction> getTransactionQueue();
    void removeFromQueue(Transaction transaction);
}
