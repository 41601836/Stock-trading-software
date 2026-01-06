package com.stock.analysis.module.largeorder;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易数据类
 * 用于Flink流处理的原始交易数据
 */
@Getter
@AllArgsConstructor
public class TradeData {
    private String stockCode;
    private int price;
    private long amount;
    private String direction;
}
