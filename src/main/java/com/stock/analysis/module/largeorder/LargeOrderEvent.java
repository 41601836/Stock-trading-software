package com.stock.analysis.module.largeorder;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class LargeOrderEvent extends ApplicationEvent {

    private final String stockCode;
    private final long amount;      // 成交金额(分)
    private final int price;        // 成交价格(分)
    private final String direction; // BUY/SELL
    private final LocalDateTime tradeTime;

    public LargeOrderEvent(Object source, String stockCode, long amount, int price, String direction) {
        super(source);
        this.stockCode = stockCode;
        this.amount = amount;
        this.price = price;
        this.direction = direction;
        this.tradeTime = LocalDateTime.now();
    }
}
