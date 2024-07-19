package edu.shtoiko.transactionprowider.service.implementation;

import edu.shtoiko.transactionprowider.model.entity.Currency;
import edu.shtoiko.transactionprowider.repository.CurrencyRepository;
import edu.shtoiko.transactionprowider.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;

    @Override
    public Mono<Currency> getById(long id) {
        return currencyRepository.findById(id);
    }

    private Mono<String> getCurrencyCodeById(long currencyId) {
        return getById(currencyId)
                .map(Currency::getCode);
    }
}
