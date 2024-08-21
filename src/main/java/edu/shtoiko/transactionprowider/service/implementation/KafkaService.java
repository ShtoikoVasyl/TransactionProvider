package edu.shtoiko.transactionprowider.service.implementation;

import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.model.entity.WithdrawResult;
import edu.shtoiko.transactionprowider.model.entity.WithdrawalTransaction;
import edu.shtoiko.transactionprowider.model.enums.TransactionStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaService {
    private final KafkaReceiver<String, Transaction> kafkaTransactionReceiver;
    private final KafkaReceiver<String, WithdrawalTransaction> kafkaWithdrawReceiver;
    private final TransactionServiceImpl transactionService;
    private final KafkaSender<String, WithdrawResult> kafkaSender;
    private final ModelMapper modelMapper;
    @Value("${kafka.withdraw-result.topic}")
    private String withdrawalResultTopic;

    @PostConstruct
    public void consumeTransactionMessages() {
        Flux<ReceiverRecord<String, Transaction>> kafkaFlux = kafkaTransactionReceiver.receive();
        kafkaFlux
            .doOnNext(record -> {
                Transaction transaction = record.value();
                log.info("Received transaction: " + transaction);
                transactionService.provideTransaction(transaction)
                    .doOnSuccess(result -> log.info("Transaction processed successfully: " + result))
                    .doOnError(error -> log.error("Transaction processing error: " + error.getMessage()))
                    .subscribe();
                record.receiverOffset().acknowledge();
            })
            .doOnError(error -> log.error("Transaction error: " + error.getMessage()))
            .subscribe();
    }

    @PostConstruct
    public void consumeWithdrawMessages() {
        Flux<ReceiverRecord<String, WithdrawalTransaction>> kafkaFlux = kafkaWithdrawReceiver.receive();
        kafkaFlux
            .doOnNext(record -> {
                WithdrawalTransaction withdrawalTransaction = record.value();
                log.info("Received withdrawal transaction: " + withdrawalTransaction);

                transactionService.provideWithdraw(withdrawalTransaction)
                    .flatMap(success -> {
                        WithdrawResult withdrawResult = modelMapper.map(withdrawalTransaction, WithdrawResult.class);
                        if (success) {
                            withdrawResult.setAllowedAmount(withdrawalTransaction.getAmount());
                            withdrawResult.setTransactionStatus(TransactionStatus.COMPLETED);
                            withdrawResult.setSystemComment(String.format(
                                "WithdrawalTransaction {} : The transaction was successful, the amount {} was withdrawn",
                                withdrawalTransaction.getRequestIdentifier(), withdrawalTransaction.getAmount()));
                        } else {
                            withdrawResult.setAllowedAmount(BigDecimal.ZERO);
                            withdrawResult.setTransactionStatus(TransactionStatus.CANCELED);
                            withdrawResult.setSystemComment("Not enough amount");
                            log.error("WithdrawalTransaction {} : not enough amount",
                                withdrawalTransaction.getRequestIdentifier());
                        }
                        return sendMessage(withdrawalResultTopic, withdrawalTransaction.getProducerIdentifier(),
                            withdrawResult);
                    })
                    .doOnError(error -> log.error("WithdrawalTransaction {} : error {}",
                        withdrawalTransaction.getRequestIdentifier(), error.getMessage()))
                    .subscribe(result -> log.info("WithdrawalTransaction {} : processed and result sent successfully",
                        withdrawalTransaction.getRequestIdentifier()));

                record.receiverOffset().acknowledge();
            })
            .doOnError(error -> log.error("Withdraw error: " + error.getMessage()))
            .subscribe();
    }

    public Mono<Void> sendMessage(String topic, String key, WithdrawResult transaction) {
        SenderRecord<String, WithdrawResult, String> senderRecord = SenderRecord.create(
            new ProducerRecord<>(topic, key, transaction), key);

        return kafkaSender.send(Flux.just(senderRecord))
            .doOnError(e -> log.error("Error sending message to topic: {}", topic, e))
            .doOnNext(r -> log.info("Message {} sent successfully to topic: {}", transaction, topic))
            .then();
    }
}
