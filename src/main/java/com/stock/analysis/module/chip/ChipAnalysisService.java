package com.stock.analysis.module.chip;

import com.stock.analysis.module.chip.StockChip;

/**
 * 筹码分布分析服务
 */
public interface ChipAnalysisService {

    /**
     * 计算股票的筹码分布 (WAD算法)
     * @param stockCode 股票代码
     * @return 筹码分布分析结果
     */
    StockChip calculateDistribution(String stockCode);
}
