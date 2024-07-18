package edu.shtoiko.transactionprowider.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@JsonFormat
@ToString
public class CurrencyExchange {
    private int r030;
    private String txt;
    private BigDecimal rate;
    private String cc;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    private LocalDate exchangedate;
}
