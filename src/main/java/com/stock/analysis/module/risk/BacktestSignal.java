package com.stock.analysis.module.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 回测信号模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestSignal implements Serializable {
    private String stockCode;
    private LocalDate signalDate;
    private double score;
    private int signalPrice;
}
