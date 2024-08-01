package edu.shtoiko.transactionprowider.model.entity;

import edu.shtoiko.transactionprowider.model.enums.TransactionStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document
//@Data
//@Table(name = "transaction")
@ToString
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

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
}
