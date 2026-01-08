package com.stock.analysis.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 股票基础信息模型
 */
@Data
@Accessors(chain = true)
public class StockBase implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 股票代码
     */
    private String stockCode;
    
    /**
     * 股票名称
     */
    private String stockName;
    
    /**
     * 所属行业
     */
    private String industry;
    
    /**
     * 所属概念
     */
    private String concept;
    
    /**
     * 上市日期（yyyy-MM-dd HH:mm:ss）
     */
    private String listingDate;
    
    /**
     * 总股本（万股）
     */
    private Long totalShares;
    
    /**
     * 流通股本（万股）
     */
    private Long circulatingShares;
}