package com.stock.analysis.module.chip;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 筹码分布实体类
 * 对应表: t_stock_chip
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockChip implements Serializable {

    private Long id;

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 分析时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime analyzeTime;

    /**
     * 当前价格 (单位: 分)
     */
    private Integer currentPrice;

    /**
     * 主筹成本 (单位: 分)
     */
    private Integer mainCost;

    /**
     * 核心筹码区间下限 (单位: 分)
     */
    private Integer priceRangeLow;

    /**
     * 核心筹码区间上限 (单位: 分)
     */
    private Integer priceRangeHigh;

    /**
     * 区间筹码占比 (如 0.8500 表示 85%)
     */
    private BigDecimal chipRatio;

    /**
     * 90%筹码集中度
     */
    private BigDecimal concentrationRatio;

    /**
     * 主筹峰值价格 (单位: 分)
     */
    private Integer peakPrice;

    /**
     * 支撑位 (单位: 分)
     */
    private Integer supportLevel;

    /**
     * 压力位 (单位: 分)
     */
    private Integer resistanceLevel;

    /**
     * 是否单峰密集
     */
    private Boolean isSinglePeak;

    /**
     * 是否低位
     */
    private Boolean isLowPosition;

    /**
     * 是否低位单峰密集
     */
    private Boolean isSinglePeakLow;

    /**
     * 详细筹码分布 (价格 -> 加权筹码量)
     */
    private Map<Integer, Double> detailedDistribution;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
