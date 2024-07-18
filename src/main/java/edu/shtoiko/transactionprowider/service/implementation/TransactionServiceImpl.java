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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

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
        return transactionRepository.save(transaction);
    }

    private boolean checkStatus(AccountVo accountVo){
        return accountVo.getStatus() == AccountStatus.OK;
    }

    @Override
    public Mono<Boolean> provideTransaction(Transaction transaction) {
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

                                    BigDecimal newReceiverAmount = receiverAccount.getAmount().add(conversionResult.getReceiverAmount());
                                    receiverAccount.setAmount(newReceiverAmount);
                                    transaction.setTransactionStatus(TransactionStatus.COMPLETED);
                                    return Mono.zip(saveAccountVo(senderAccount), saveAccountVo(receiverAccount))
                                            .then(transactionRepository.save(transaction))
                                            .thenReturn(true);
                                });
                    } else {
                        return Mono.error(new RuntimeException("One of accounts is blocked"));
                    }
                });
    }

    private Mono<AccountVo> saveAccountVo(AccountVo accountVo){
        System.out.println("account saved. Number:" + accountVo);
        return accountRepository.save(accountVo);
    }
}
