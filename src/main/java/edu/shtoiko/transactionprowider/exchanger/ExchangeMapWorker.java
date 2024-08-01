package edu.shtoiko.transactionprowider.exchanger;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.shtoiko.transactionprowider.model.entity.CurrencyExchange;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Getter
@RequiredArgsConstructor
public class ExchangeMapWorker {
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final Map<String, CurrencyExchange> currencyExchangeMap = new HashMap<>();

    @Value("${conversionservice.exchangeRateURL}")
    private String exchangeRateSource;

    @Value("${conversion.updatetime}")
    private int updateInterruption;

    @PostConstruct
    public void init() {
        CurrencyExchange uah = new CurrencyExchange();
        uah.setRate(new BigDecimal(1));
        uah.setCc("UAH");
        currencyExchangeMap.put("UAH", uah);
        updateExchangeRates();
    }

    @Scheduled(fixedRateString = "${conversion.updatetime}")
    public void updateExchangeRates() {
        log.info("Updating exchange rates");
        getCurrencyExchanges().subscribe(
            currencyExchange -> currencyExchangeMap.put(currencyExchange.getCc(), currencyExchange),
            error -> log.error("Error updating exchange rates", error));
    }

    public Flux<CurrencyExchange> getCurrencyExchanges() {
        return webClientBuilder.build().get().uri(exchangeRateSource)
            .retrieve()
            .bodyToFlux(CurrencyExchange.class);
    }
}
