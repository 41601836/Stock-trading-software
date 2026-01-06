package com.stock.analysis.module.market;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实时行情数据实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeQuote implements Serializable {

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 股票名称
     */
    private String stockName;

    /**
     * 当前价格 (分)
     */
    private int currentPrice;

    /**
     * 开盘价 (分)
     */
    private int openPrice;

    /**
     * 最高价 (分)
     */
    private int highPrice;

    /**
     * 最低价 (分)
     */
    private int lowPrice;

    /**
     * 昨收价 (分)
     */
    private int prevClosePrice;

    /**
     * 成交量 (股)
     */
    private long volume;

    /**
     * 成交额 (分)
     */
    private long amount;

    /**
     * 涨跌幅 (%)
     */
    private double changePercent;

    /**
     * 涨跌额 (分)
     */
    private int changeAmount;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}