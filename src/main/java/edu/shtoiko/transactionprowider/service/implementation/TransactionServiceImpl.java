package edu.shtoiko.transactionprowider.service.implementation;

import edu.shtoiko.transactionprowider.model.entity.AccountVo;
import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.model.enums.AccountStatus;
import edu.shtoiko.transactionprowider.model.enums.TransactionStatus;
import edu.shtoiko.transactionprowider.repository.AccountRepository;
import edu.shtoiko.transactionprowider.repository.TransactionRepository;
import edu.shtoiko.transactionprowider.service.ConversionService;
import edu.shtoiko.transactionprowider.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final ConversionService conversionService;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Override
    public Flux<Transaction> getAllTransactionByStatus(TransactionStatus transactionStatus) {
        return transactionRepository.findByTransactionStatus(transactionStatus);
    }

    public Flux<Transaction> processNewTransactions() {
        return getAllTransactionByStatus(TransactionStatus.NEW)
                .flatMap(this::processTransaction);
    }

    private Mono<Transaction> processTransaction(Transaction transaction) {
        transaction.setTransactionStatus(TransactionStatus.IN_PROGRESS);
        log.info("Processing transaction: {}", transaction.getId());
        return transactionRepository.save(transaction);
    }

    private boolean checkStatus(AccountVo accountVo) {
        boolean statusOk = accountVo.getStatus() == AccountStatus.OK;
        log.debug("Account status for account number {}: {}", accountVo.getAccountNumber(), accountVo.getStatus());
        return statusOk;
    }

    @Override
    public Mono<Boolean> provideTransaction(Transaction transaction) {
        log.info("Providing transaction: {}", transaction.getId());
        Mono<AccountVo> receiverMono = accountRepository.findByAccountNumber(transaction.getReceiverAccountNumber());
        Mono<AccountVo> senderMono = accountRepository.findByAccountNumber(transaction.getSenderAccountNumber());

        return Mono.zip(receiverMono, senderMono)
                .flatMap(tuple -> {
                    AccountVo receiverAccount = tuple.getT1();
                    AccountVo senderAccount = tuple.getT2();

                    if (checkStatus(receiverAccount) && checkStatus(senderAccount)) {
                        return conversionService.convertCurrency(
                                        senderAccount.getCurrencyId(), receiverAccount.getCurrencyId(), transaction.getAmount(), transaction.getCurrencyCode())
                                .flatMap(conversionResult -> {
                                    BigDecimal newSenderAmount = senderAccount.getAmount().subtract(conversionResult.getSenderAmount());
                                    senderAccount.setAmount(newSenderAmount);
                                    log.info("New sender account balance: {}", newSenderAmount);

                                    BigDecimal newReceiverAmount = receiverAccount.getAmount().add(conversionResult.getReceiverAmount());
                                    receiverAccount.setAmount(newReceiverAmount);
                                    log.info("New receiver account balance: {}", newReceiverAmount);

                                    transaction.setTransactionStatus(TransactionStatus.COMPLETED);
                                    log.info("Transaction {} completed", transaction.getId());
                                    return Mono.zip(saveAccountVo(senderAccount), saveAccountVo(receiverAccount))
                                            .then(transactionRepository.save(transaction))
                                            .thenReturn(true);
                                });
                    } else {
                        log.error("One of the accounts is blocked. Transaction {} failed", transaction.getId());
                        return Mono.error(new RuntimeException("One of accounts is blocked"));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error providing transaction: {}", transaction.getId(), e);
                    transaction.setTransactionStatus(TransactionStatus.INTERRUPTED);
                    return transactionRepository.save(transaction).thenReturn(false);
                });
    }

    private Mono<AccountVo> saveAccountVo(AccountVo accountVo) {
        log.info("Saving account. Number: {}", accountVo.getAccountNumber());
        return accountRepository.save(accountVo);
    }
}