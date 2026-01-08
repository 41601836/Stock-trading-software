package com.stock.analysis.module.heatflow;

import com.stock.analysis.common.model.StockBase;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 资金流向数据模型
 */
@Data
@Accessors(chain = true)
public class HeatFlowData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 股票基本信息
     */
    private StockBase stockBase;
    
    /**
     * 交易日期（yyyy-MM-dd HH:mm:ss）
     */
    private String tradeDate;
    
    /**
     * 主力资金净流入（分）
     */
    private Long mainNetInflow;
    
    /**
     * 超大单资金净流入（分）
     */
    private Long superLargeNetInflow;
    
    /**
     * 大单资金净流入（分）
     */
    private Long largeNetInflow;
    
    /**
     * 中单资金净流入（分）
     */
    private Long mediumNetInflow;
    
    /**
     * 小单资金净流入（分）
     */
    private Long smallNetInflow;
    
    /**
     * 主力资金占比（‰）
     */
    private Integer mainRatio;
    
    /**
     * 超大单资金占比（‰）
     */
    private Integer superLargeRatio;
    
    /**
     * 大单资金占比（‰）
     */
    private Integer largeRatio;
    
    /**
     * 中单资金占比（‰）
     */
    private Integer mediumRatio;
    
    /**
     * 小单资金占比（‰）
     */
    private Integer smallRatio;
    
    /**
     * 资金净流入排名（行业内）
     */
    private Integer industryRanking;
    
    /**
     * 资金净流入排名（概念内）
     */
    private Integer conceptRanking;
}