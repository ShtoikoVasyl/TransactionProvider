package edu.shtoiko.transactionprowider.service;

import edu.shtoiko.transactionprowider.exchanger.entity.CurrencyConversionResult;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface ConversionService {
    Mono<CurrencyConversionResult> convertCurrency(String transactionId, long senderCurrencyId, long receiverCurrencyId, BigDecimal amount,
        String currencyCode);
}
