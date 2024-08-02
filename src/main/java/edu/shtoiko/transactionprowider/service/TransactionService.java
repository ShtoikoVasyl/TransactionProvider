package edu.shtoiko.transactionprowider.service;

import edu.shtoiko.transactionprowider.model.entity.AccountVo;
import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.model.entity.WithdrawalTransaction;
import edu.shtoiko.transactionprowider.model.enums.TransactionStatus;
import reactor.core.CorePublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {
    Flux<Transaction> getAllTransactionByStatus(TransactionStatus transactionStatus);

    Mono<Boolean> provideTransaction(Transaction transaction);

    boolean checkPinCode(AccountVo account, short pinCode);

    Mono<Boolean> provideWithdraw(WithdrawalTransaction withdrawalTransaction);
}
