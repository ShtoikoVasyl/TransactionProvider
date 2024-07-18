package edu.shtoiko.transactionprowider.service;

import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.model.enums.TransactionStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {
    Flux<Transaction> getAllTransactionByStatus(TransactionStatus transactionStatus);

    Flux<Transaction> processNewTransactions();

    Mono<Boolean> provideTransaction(Transaction transaction);
}
