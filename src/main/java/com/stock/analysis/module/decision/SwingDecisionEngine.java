package com.stock.analysis.module.decision;

import com.stock.analysis.module.chip.StockChip;
import com.stock.analysis.module.largeorder.LargeOrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 波段决策引擎
 * 根据筹码分布和机构大买情况计算胜率评分
 */
@Component
@RequiredArgsConstructor
public class SwingDecisionEngine {

    // 基本胜率评分
    private static final int BASE_SCORE = 60;
    
    // 筹码低位单峰密集加分
    private static final int LOW_POSITION_SINGLE_PEAK_SCORE = 20;
    
    // 三家机构大买加分
    private static final int THREE_INSTITUTIONS_BUY_SCORE = 30;
    
    // 目标胜率评分阈值（当满足特定条件时）
    private static final int TARGET_WIN_RATE_THRESHOLD = 90;
    
    /**
     * 计算胜率评分
     * @param stockCode 股票代码
     * @param stockChip 筹码分析结果
     * @param largeOrders 近期大单事件列表
     * @return 胜率评分
     */
    public int calculateWinRateScore(String stockCode, StockChip stockChip, List<LargeOrderEvent> largeOrders) {
        // 初始分数
        int score = BASE_SCORE;
        
        // 检查是否满足筹码低位单峰密集条件
        boolean isLowPositionSinglePeak = isLowPositionSinglePeak(stockChip);
        
        // 检查是否满足三家机构大买条件
        boolean hasThreeInstitutionsBuy = hasThreeInstitutionsBuy(largeOrders);
        
        // 添加筹码低位单峰密集分数
        if (isLowPositionSinglePeak) {
            score += LOW_POSITION_SINGLE_PEAK_SCORE;
        }
        
        // 添加三家机构大买分数
        if (hasThreeInstitutionsBuy) {
            score += THREE_INSTITUTIONS_BUY_SCORE;
        }
        
        // 如果同时满足两个条件，确保胜率评分超过90%阈值
        if (isLowPositionSinglePeak && hasThreeInstitutionsBuy) {
            score = Math.max(score, TARGET_WIN_RATE_THRESHOLD);
        }
        
        // 限制分数在0-100之间
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * 检查是否满足筹码低位单峰密集条件
     * @param stockChip 筹码分析结果
     * @return 是否满足条件
     */
    private boolean isLowPositionSinglePeak(StockChip stockChip) {
        return stockChip != null && stockChip.getIsSinglePeak() && stockChip.getIsLowPosition();
    }
    
    /**
     * 检查是否满足三家机构大买条件
     * @param largeOrders 近期大单事件列表
     * @return 是否满足条件
     */
    private boolean hasThreeInstitutionsBuy(List<LargeOrderEvent> largeOrders) {
        if (largeOrders == null || largeOrders.isEmpty()) {
            return false;
        }
        
        // 统计近期三家机构大买事件
        long threeInstitutionsBuyCount = largeOrders.stream()
                .filter(order -> "三家机构大买".equals(order.getIntent()))
                .count();
        
        // 如果存在至少一次三家机构大买事件，返回true
        return threeInstitutionsBuyCount > 0;
    }
}