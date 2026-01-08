package com.stock.analysis.module.largeorder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 大单检测结果类
 * 用于返回大单检测的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LargeOrderResult {
    
    private String stockCode;
    private List<LargeOrderEvent> largeOrders;
    private long detectionTimeMs;
    private long dynamicThreshold;
    private boolean hasLargeOrders;
    
    public LargeOrderResult(String stockCode, List<LargeOrderEvent> largeOrders, long detectionTimeMs, long dynamicThreshold) {
        this.stockCode = stockCode;
        this.largeOrders = largeOrders;
        this.detectionTimeMs = detectionTimeMs;
        this.dynamicThreshold = dynamicThreshold;
        this.hasLargeOrders = largeOrders != null && !largeOrders.isEmpty();
    }
}
