package edu.shtoiko.transactionprowider.service.implementation;

import edu.shtoiko.transactionprowider.model.entity.Transaction;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final KafkaReceiver<String, Transaction> kafkaReceiver;

    private final TransactionServiceImpl transactionService;

    @PostConstruct
    public void consumeMessages() {
        Flux<ReceiverRecord<String, Transaction>> kafkaFlux = kafkaReceiver.receive();
        kafkaFlux.subscribe(record -> {
            Transaction transaction = record.value();
            log.info("Received transaction: " + transaction);
            Disposable subscribe = transactionService.provideTransaction(transaction).subscribe();
            record.receiverOffset().acknowledge();
        });
    }
}
