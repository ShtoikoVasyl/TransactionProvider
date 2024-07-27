package edu.shtoiko.transactionprowider.service.implementation;

import edu.shtoiko.transactionprowider.exchanger.ExchangeMapWorker;
import edu.shtoiko.transactionprowider.exchanger.entity.CurrencyConversionResult;
import edu.shtoiko.transactionprowider.model.entity.Currency;
import edu.shtoiko.transactionprowider.model.entity.CurrencyExchange;
import edu.shtoiko.transactionprowider.service.CurrencyService;
import edu.shtoiko.transactionprowider.service.ConversionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
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
    public Mono<CurrencyConversionResult> convertCurrency(String transactionId, long senderCurrencyId, long receiverCurrencyId,
        BigDecimal amount, String transactionCurrencyCode) {
        log.info(
            "Transaction {} : converting currency senderCurrencyId={}, receiverCurrencyId={}, amount={}, transactionCurrencyCode={}", transactionId,
            senderCurrencyId, receiverCurrencyId, amount, transactionCurrencyCode);

        Mono<String> senderCurrencyCodeMono = currencyService.getById(senderCurrencyId)
            .map(Currency::getCode);
        Mono<String> receiverCurrencyCodeMono = currencyService.getById(receiverCurrencyId)
            .map(Currency::getCode);

        return Mono.zip(senderCurrencyCodeMono, receiverCurrencyCodeMono)
            .flatMap(tuple -> {
                String senderCurrencyCode = tuple.getT1();
                String receiverCurrencyCode = tuple.getT2();

                CurrencyExchange senderExchangeRate = currencyExchangeMap.get(senderCurrencyCode);
                CurrencyExchange receiverExchangeRate = currencyExchangeMap.get(receiverCurrencyCode);
                CurrencyExchange transactionExchangeRate = currencyExchangeMap.get(transactionCurrencyCode);

                if (senderExchangeRate == null || receiverExchangeRate == null || transactionExchangeRate == null) {
                    String errorMessage =
                        "Transaction " + transactionId + ": exchange rate not found for one or more currencies: senderCurrencyCode=" + senderCurrencyCode +
                            ", receiverCurrencyCode=" + receiverCurrencyCode + ", transactionCurrencyCode="
                            + transactionCurrencyCode;
                    log.error(errorMessage);
                    return Mono.error(new RuntimeException(errorMessage));
                }

                BigDecimal senderAmount = convertToCurrency(senderExchangeRate, amount, transactionExchangeRate);
                BigDecimal receiverAmount = convertToCurrency(receiverExchangeRate, amount, transactionExchangeRate);

                CurrencyConversionResult result = new CurrencyConversionResult(senderAmount, receiverAmount);
                log.info("Transaction {} : currency conversion result senderAmount={}, receiverAmount={}", transactionId , senderAmount,
                    receiverAmount);
                return Mono.just(result);
            });
    }

    private BigDecimal convertToCurrency(CurrencyExchange targetCurrencyExchangeRate, BigDecimal amount,
        CurrencyExchange sourceExchangeRate) {
        BigDecimal amountInUah = amount.multiply(sourceExchangeRate.getRate());
        BigDecimal targetAmount = amountInUah.divide(targetCurrencyExchangeRate.getRate(), 2, RoundingMode.HALF_UP);
        log.debug("Converting amount: amount={}, sourceRate={}, targetRate={}, result={}",
            amount, sourceExchangeRate.getRate(), targetCurrencyExchangeRate.getRate(), targetAmount);
        return targetAmount;
    }
}