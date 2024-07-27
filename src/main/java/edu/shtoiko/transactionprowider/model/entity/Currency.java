package edu.shtoiko.transactionprowider.model.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name = "currency")
public class Currency {

    @Id
    private Long id;

    @Column
    private String code;

    @Column
    private String fullName;

    @Column
    private String sign;
}
