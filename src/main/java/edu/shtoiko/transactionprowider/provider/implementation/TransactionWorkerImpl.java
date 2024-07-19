package edu.shtoiko.transactionprowider.provider.implementation;

import edu.shtoiko.transactionprowider.exchanger.CurrencyExchangeCalculator;
import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.provider.transactionstack.TransactionStackProvider;
import edu.shtoiko.transactionprowider.service.TransactionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class TransactionWorkerImpl {
    private final CurrencyExchangeCalculator currencyExchangeCalculator;

    private final TransactionStackProvider transactionStackProvider;

    private final TransactionService transactionService;

    @Value("${transactionWorker.interruptionTime}")
    private long interruptionTime;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @PostConstruct
    public void init() {
        executorService.submit(this::runInBackground);
    }

    private void runInBackground(){
        while (true) {
            transactionStackProvider.getTransactionQueue().subscribe(transaction -> {
                Disposable subscribe = provideTransaction(transaction).subscribe(isTrue -> {
                    if (isTrue) {
                        transactionStackProvider.removeFromQueue(transaction);
                    }
                });
            });

            try {
                Thread.sleep(interruptionTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        }

    public Mono<Boolean> provideTransaction(Transaction transaction){
        return transactionService.provideTransaction(transaction);
    }
}
