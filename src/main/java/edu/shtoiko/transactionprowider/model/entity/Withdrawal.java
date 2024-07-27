package edu.shtoiko.transactionprowider.model.entity;

import edu.shtoiko.transactionprowider.model.enums.TransactionStatus;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.Instant;

public class Withdrawal extends FinancialOperation {
    @Id
    private String id;

    private Instant date;

    private Long receiverAccountNumber;

    private Long senderAccountNumber;

    private BigDecimal amount;

    private String currencyCode;

    private String description;

    private TransactionStatus transactionStatus;

    private String systemComment;

    private String terminalId;
}
