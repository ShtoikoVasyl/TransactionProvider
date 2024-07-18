package edu.shtoiko.transactionprowider.model.entity;

import edu.shtoiko.transactionprowider.model.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table(name = "accounts")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class Account {
    public Account() {
    }

    private long id;

    private Long ownerId;

    private String accountName;

    private long accountNumber;

    private Currency currency;

    private BigDecimal amount;

    private AccountStatus accountStatus;
}
