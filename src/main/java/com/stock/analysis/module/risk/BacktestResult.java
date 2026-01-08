package com.stock.analysis.module.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 回测结果模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResult implements Serializable {
    private String stockCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalSignals;
    private List<TradeResult> tradeResults;
    private double winRate5Day;
    private double profitLossRatio5Day;
    private double winRate10Day;
    private double profitLossRatio10Day;
}
