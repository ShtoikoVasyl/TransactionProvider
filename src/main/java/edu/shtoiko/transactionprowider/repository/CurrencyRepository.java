package edu.shtoiko.transactionprowider.repository;

import edu.shtoiko.transactionprowider.model.entity.Currency;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRepository extends ReactiveCrudRepository<Currency, Long> {
    Currency findByCode(String code);
}
