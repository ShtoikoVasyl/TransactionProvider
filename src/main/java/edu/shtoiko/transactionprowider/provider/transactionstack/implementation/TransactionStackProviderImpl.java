package edu.shtoiko.transactionprowider.provider.transactionstack.implementation;

import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.provider.transactionstack.TransactionStackProvider;
import edu.shtoiko.transactionprowider.service.TransactionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class TransactionStackProviderImpl implements TransactionStackProvider {

    private final TransactionService transactionService;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${transactionStakProwider.updatetime}")
    private int updateInterruption;

    @PostConstruct
    public void init() {
        executorService.submit(this::runInBackground);
    }

    private final ConcurrentLinkedQueue<Transaction> transactionQueue = new ConcurrentLinkedQueue<>();

    private void runInBackground() {
        while (true) {
            transactionService.processNewTransactions().subscribe(transactionQueue::add);
            try {
                Thread.sleep(updateInterruption);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void removeFromQueue(Transaction transaction) {
        transactionQueue.remove(transaction);
        System.out.println("removed from queue id:" + transaction.getId());
    }

    public Flux<Transaction> getTransactionQueue(){
        return Flux.fromIterable(transactionQueue);
    }
}
