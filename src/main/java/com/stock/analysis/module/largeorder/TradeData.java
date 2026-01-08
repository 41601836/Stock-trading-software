package com.stock.analysis.module.largeorder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 交易数据类
 * 用于Flink流处理的原始交易数据
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeData {
    private String stockCode;
    private int price;
    private long amount;
    private String direction;
    private String seatName; // 席位名称
    private String traderType; // 交易者类型：机构/游资/散户
    private String tradeTime; // 交易时间
}

