package edu.shtoiko.transactionprowider.exchanger.entity;

import java.math.BigDecimal;

public class CurrencyConversionResult {
    private final BigDecimal senderAmount;
    private final BigDecimal receiverAmount;

    public CurrencyConversionResult(BigDecimal senderAmount, BigDecimal receiverAmount) {
        this.senderAmount = senderAmount;
        this.receiverAmount = receiverAmount;
    }

    public BigDecimal getSenderAmount() {
        return senderAmount;
    }

    public BigDecimal getReceiverAmount() {
        return receiverAmount;
    }
}
