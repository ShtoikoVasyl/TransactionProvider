package edu.shtoiko.transactionprowider.model.entity;

import edu.shtoiko.transactionprowider.model.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table(name = "accounts")
@Getter
@Setter
@AllArgsConstructor
@Builder
@ToString
public class AccountVo {
    public AccountVo() {
    }

    @Id
    private long id;

    @Column
    private Long ownerId;

    @Column
    private String accountName;

    @Column
    private long accountNumber;

    @Column
    private short pinCode;

    @Column
    private long currencyId;

    @Column
    private BigDecimal amount;

    @Column
    private AccountStatus status;
}
