package com.stock.analysis.module.largeorder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class LargeOrderEvent extends ApplicationEvent {

    private final String stockCode;
    private final long amount;      // 成交金额(分)
    private final int price;        // 成交价格(分)
    private final String direction; // BUY/SELL
    private final String intent;    // 交易意图
    private final LocalDateTime tradeTime;

    public LargeOrderEvent(Object source, String stockCode, long amount, int price, String direction, String intent) {
        super(source);
        this.stockCode = stockCode;
        this.amount = amount;
        this.price = price;
        this.direction = direction;
        this.intent = intent;
        this.tradeTime = LocalDateTime.now();
    }
}

// 用于存储大单信息的DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
class LargeOrderInfo {
    private String stockCode;
    private long amount;
    private int price;
    private String direction;
    private String intent;
    private LocalDateTime tradeTime;
}
