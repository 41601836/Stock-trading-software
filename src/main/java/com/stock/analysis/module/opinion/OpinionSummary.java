package com.stock.analysis.module.opinion;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class OpinionSummary implements Serializable {
    
    private String stockCode;
    private int totalCount;
    
    // 情感比例 (0.00 - 1.00)
    private BigDecimal positiveRatio; // > 60分
    private BigDecimal negativeRatio; // < 40分
    private BigDecimal neutralRatio;  // 40-60分
    
    // 平均情感得分
    private int avgSentimentScore;
    
    // 是否触发热点异动
    private boolean isHotEvent;
    private String hotEventMsg;
}
