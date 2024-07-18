package edu.shtoiko.transactionprowider.service;

import edu.shtoiko.transactionprowider.model.entity.Currency;
import reactor.core.publisher.Mono;

public interface CurrencyService {
    Mono<Currency> getById(long id);
}
