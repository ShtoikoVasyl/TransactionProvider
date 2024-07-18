package edu.shtoiko.transactionprowider.service.implementation;

import edu.shtoiko.transactionprowider.exchanger.ExchangeMapWorker;
import edu.shtoiko.transactionprowider.exchanger.entity.CurrencyConversionResult;
import edu.shtoiko.transactionprowider.model.entity.CurrencyExchange;
import edu.shtoiko.transactionprowider.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import edu.shtoiko.transactionprowider.service.ConversionService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConversionServiceImpl implements ConversionService {

    private final CurrencyService currencyService;

    private final ExchangeMapWorker exchangeMapWorker;

    private Map<String, CurrencyExchange> currencyExchangeMap = null;

    @PostConstruct
    public void init() {
        currencyExchangeMap = exchangeMapWorker.getCurrencyExchangeMap();
    }

    @Override
    public Mono<CurrencyConversionResult> convertCurrency(long senderCurrencyId, long receiverCurrencyId, BigDecimal amount, String transactionCurrencyCode) {
        Mono<String> senderCurrencyCodeMono = currencyService.getById(senderCurrencyId)
                .map(currency -> currency.getCode());
        Mono<String> receiverCurrencyCodeMono = currencyService.getById(receiverCurrencyId)
                .map(currency -> currency.getCode());
        return Mono.zip(senderCurrencyCodeMono, receiverCurrencyCodeMono)
                .flatMap(tuple -> {
                    String receiverCurrencyCode = tuple.getT1();
                    String senderCurrencyCode = tuple.getT2();
                    CurrencyExchange senderExchangeRate = currencyExchangeMap.get(senderCurrencyCode);
                    CurrencyExchange receiverExchangeRate = currencyExchangeMap.get(receiverCurrencyCode);
                    CurrencyExchange transactionExchangeRate = currencyExchangeMap.get(transactionCurrencyCode);

                    if (senderExchangeRate == null || receiverExchangeRate == null || transactionExchangeRate == null) {
                        return Mono.error(new RuntimeException("Exchange rate not found for one or more currencies"));
                    }

                    BigDecimal senderAmount = convertToCurrency(senderExchangeRate, amount, transactionExchangeRate);
                    BigDecimal receiverAmount = convertToCurrency(receiverExchangeRate, amount, transactionExchangeRate);

                    CurrencyConversionResult result = new CurrencyConversionResult(senderAmount, receiverAmount);
                    return Mono.just(result);
                });
    }

    private BigDecimal convertToCurrency(CurrencyExchange targetCurrencyExchangeRate, BigDecimal amount, CurrencyExchange sourceExchangeRate) {
        BigDecimal amountInUah = amount.multiply(sourceExchangeRate.getRate());
        BigDecimal targetAmount = amountInUah.divide(targetCurrencyExchangeRate.getRate(), 2, RoundingMode.HALF_UP);
        return targetAmount;
    }
}
