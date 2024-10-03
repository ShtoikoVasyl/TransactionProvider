package edu.shtoiko.transactionprowider.service.implementation;

import edu.shtoiko.transactionprowider.model.entity.AccountVo;
import edu.shtoiko.transactionprowider.model.entity.Transaction;
import edu.shtoiko.transactionprowider.model.entity.WithdrawalTransaction;
import edu.shtoiko.transactionprowider.model.enums.AccountStatus;
import edu.shtoiko.transactionprowider.model.enums.ProcessingStatus;
import edu.shtoiko.transactionprowider.model.enums.TransactionStatus;
import edu.shtoiko.transactionprowider.repository.AccountRepository;
import edu.shtoiko.transactionprowider.repository.TransactionRepository;
import edu.shtoiko.transactionprowider.service.ConversionService;
import edu.shtoiko.transactionprowider.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final ConversionService conversionService;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;

    @Value("${transactionservice.deafultbankserviceaccountnumber}")
    private long bankServiceAccount;

    @Override
    public Flux<Transaction> getAllTransactionByStatus(TransactionStatus transactionStatus) {
        return transactionRepository.findByTransactionStatus(transactionStatus);
    }

    @Override
    public boolean checkPinCode(AccountVo account, short pinCode) {
        return account.getPinCode() == pinCode;
    }

    private Transaction convertWithdrawToTransaction(WithdrawalTransaction withdrawalTransaction) {
        Transaction newTransaction = modelMapper.map(withdrawalTransaction, Transaction.class);
        newTransaction.setReceiverAccountNumber(bankServiceAccount);
        newTransaction.setId(withdrawalTransaction.getRequestIdentifier());
        return newTransaction;
    }

    @Override
    public Mono<Boolean> provideWithdraw(WithdrawalTransaction withdrawalTransaction) {
        log.debug("Transaction {}: provide started", withdrawalTransaction.getRequestIdentifier());
        Mono<AccountVo> senderMono =
            accountRepository.findByAccountNumber(withdrawalTransaction.getSenderAccountNumber());
        return senderMono.flatMap(
            accountVo -> {
                if (accountVo.getPinCode() == withdrawalTransaction.getPinCode()) {
                    log.debug("Transaction {}: pin matches", withdrawalTransaction.getRequestIdentifier());
                    return provideTransaction(convertWithdrawToTransaction(withdrawalTransaction));
                } else {
                    return Mono.just(false);
                }
            });
    }

    private boolean checkStatus(AccountVo accountVo) {
        boolean statusOk = accountVo.getStatus() == AccountStatus.OK;
        log.debug("Account status for account number {}: {}", accountVo.getAccountNumber(), accountVo.getStatus());
        return statusOk;
    }

    @Override
    public Mono<Boolean> provideTransaction(Transaction transaction) {
        log.info("Transaction {} : providing started", transaction.getId());

        Mono<AccountVo> senderMono = accountRepository.findAndUpdateStatusIfReady(transaction.getSenderAccountNumber())
            .switchIfEmpty(Mono.error(() -> {
                log.error("Transaction {} : sender account in progress", transaction.getId());
                return new RuntimeException("Entities are still processing");
            }));
        Mono<AccountVo> receiverMono =
            accountRepository.findAndUpdateStatusIfReady(transaction.getReceiverAccountNumber())
                .switchIfEmpty(Mono.error(() -> {
                    log.error("Transaction {} : receiver account in progress", transaction.getId());
                    return new RuntimeException("Entities are still processing");
                }));
        return Mono.zip(receiverMono, senderMono)
            .flatMap(tuple -> {
                AccountVo receiverAccount = tuple.getT1();
                AccountVo senderAccount = tuple.getT2();

                if (checkStatus(receiverAccount) && checkStatus(senderAccount)) {
                    return conversionService.convertCurrency(transaction.getId(),
                        senderAccount.getCurrencyId(), receiverAccount.getCurrencyId(),
                        transaction.getAmount(), transaction.getCurrencyCode())
                        .flatMap(conversionResult -> {
                            BigDecimal newSenderAmount =
                                senderAccount.getAmount().subtract(conversionResult.getSenderAmount());
                            if (newSenderAmount.compareTo(BigDecimal.ZERO) < 0) {
                                log.error("Transaction {} : sender {} does not have enough funds. Current balance {}",
                                    transaction.getId(), senderAccount.getAccountNumber(), senderAccount.getAmount());
                                transaction.setTransactionStatus(TransactionStatus.CANCELED);
                                transaction.setSystemComment("Not enough value");
                                return Mono
                                    .zip(saveAccountVo(transaction.getId(), senderAccount),
                                        saveAccountVo(transaction.getId(), receiverAccount))
                                    .then(transactionRepository.save(transaction))
                                    .thenReturn(false);
                            }

                            senderAccount.setAmount(newSenderAmount);
                            log.info("Transaction {} : new sender account balance: {}", transaction.getId(),
                                newSenderAmount);

                            BigDecimal newReceiverAmount =
                                receiverAccount.getAmount().add(conversionResult.getReceiverAmount());
                            receiverAccount.setAmount(newReceiverAmount);
                            log.info("Transaction {} : new receiver account balance: {}", transaction.getId(),
                                newReceiverAmount);

                            transaction.setTransactionStatus(TransactionStatus.COMPLETED);
                            log.info("Transaction {} : completed", transaction.getId());

                            return Mono
                                .zip(saveAccountVo(transaction.getId(), senderAccount),
                                    saveAccountVo(transaction.getId(), receiverAccount))
                                .then(transactionRepository.save(transaction))
                                .thenReturn(true);
                        });
                } else {
                    log.error("Transaction {} : failed, one of the accounts is blocked. ", transaction.getId());
                    return Mono.error(new RuntimeException("One of accounts is blocked"));
                }
            })
            .retryWhen(Retry.backoff(5, Duration.ofSeconds(1))
                .filter(throwable -> throwable instanceof RuntimeException &&
                    throwable.getMessage().equals("Entities are still processing"))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Transaction {} : exceeded maximum retry attempts", transaction.getId());
                    return new RuntimeException("Exceeded maximum retry attempts");
                }))
            .onErrorResume(throwable -> {
                if (throwable.getMessage().equals("Exceeded maximum retry attempts")) {
                    return performFallbackLogic(transaction).thenReturn(false);
                }
                return Mono.error(throwable);
            });
    }

    private Mono<Transaction> performFallbackLogic(Transaction transaction) {
        log.error("Transaction {} : exceeded maximum retry attempts. Performing fallback logic...",
            transaction.getId());
        transaction.setTransactionStatus(TransactionStatus.INTERRUPTED);
        return transactionRepository.save(transaction);
    }

    private Mono<AccountVo> saveAccountVo(String transactionId, AccountVo accountVo) {
        log.info("Transaction {} : saving account. Number: {}", transactionId, accountVo.getAccountNumber());
        accountVo.setProcessingStatus(ProcessingStatus.READY);
        return accountRepository.save(accountVo);
    }
}