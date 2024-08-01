package edu.shtoiko.transactionprowider.model.entity;

import edu.shtoiko.transactionprowider.model.enums.TransactionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@NoArgsConstructor
@ToString
public class WithdrawalTransaction {
    private String requestIdentifier;

    private String producerIdentifier;

    private Instant date;

    private Long senderAccountNumber;

    private int pinCode;

    private BigDecimal amount;

    private String currencyCode;

    private String description;

    private TransactionStatus transactionStatus;

    private String systemComment;
}
