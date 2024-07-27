package edu.shtoiko.transactionprowider.repository;

import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.model.enums.TransactionStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {
    Flux<Transaction> findByTransactionStatus(TransactionStatus transactionStatus);

}
