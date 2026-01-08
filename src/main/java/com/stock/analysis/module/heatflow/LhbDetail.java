package com.stock.analysis.module.heatflow;

import com.stock.analysis.common.model.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 龙虎榜详情模型
 */
@Data
@Accessors(chain = true)
public class LhbDetail extends BaseEntity {
    
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
     * 交易日期
     */
    private String tradeDate;
    
    /**
     * 营业部名称
     */
    private String departmentName;
    
    /**
     * 席位代码
     */
    private String seatCode;
    
    /**
     * 买入金额（分）
     */
    private Long buyAmount;
    
    /**
     * 卖出金额（分）
     */
    private Long sellAmount;
    
    /**
     * 净买入金额（分）
     */
    private Long netBuyAmount;
    
    /**
     * 买入占比（‰）
     */
    private Integer buyRatio;
    
    /**
     * 卖出占比（‰）
     */
    private Integer sellRatio;
    
    /**
     * 席位类型：1-顶级游资，2-机构专用，3-普通席位
     */
    private Integer seatType;
    
    /**
     * 上榜理由
     */
    private String reason;
}