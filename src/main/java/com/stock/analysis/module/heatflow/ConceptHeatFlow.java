package com.stock.analysis.module.heatflow;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 概念资金流向模型
 */
@Data
@Accessors(chain = true)
public class ConceptHeatFlow implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 概念名称
     */
    private String concept;
    
    /**
     * 资金净流入（分）
     */
    private Long netInflow;
    
    /**
     * 资金净流出（分）
     */
    private Long netOutflow;
    
    /**
     * 资金流入总额（分）
     */
    private Long totalInflow;
    
    /**
     * 资金流出总额（分）
     */
    private Long totalOutflow;
    
    /**
     * 净流入占比（‰）
     */
    private Integer inflowRatio;
    
    /**
     * 上涨股票数
     */
    private Integer riseStockCount;
    
    /**
     * 下跌股票数
     */
    private Integer fallStockCount;
    
    /**
     * 平盘股票数
     */
    private Integer flatStockCount;
    
    /**
     * 平均涨跌幅（‰）
     */
    private Integer avgPriceChangeRatio;
    
    /**
     * 排名
     */
    private Integer ranking;
}