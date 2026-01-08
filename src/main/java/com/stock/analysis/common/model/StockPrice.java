package com.stock.analysis.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 股票价格数据模型
 * 价格使用Integer类型，单位为分
 */
@Data
@Accessors(chain = true)
public class StockPrice implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 股票代码
     */
    private String stockCode;
    
    /**
     * 交易时间（yyyy-MM-dd HH:mm:ss）
     */
    private String tradeTime;
    
    /**
     * 开盘价（分）
     */
    private Integer openPrice;
    
    /**
     * 收盘价（分）
     */
    private Integer closePrice;
    
    /**
     * 最高价（分）
     */
    private Integer highPrice;
    
    /**
     * 最低价（分）
     */
    private Integer lowPrice;
    
    /**
     * 成交量（股）
     */
    private Long volume;
    
    /**
     * 成交额（分）
     */
    private Long amount;
    
    /**
     * 涨跌额（分）
     */
    private Integer priceChange;
    
    /**
     * 涨跌幅（‰，千分比）
     */
    private Integer priceChangeRatio;
}