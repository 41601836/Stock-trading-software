package com.stock.analysis.module.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 交易结果模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResult implements Serializable {
    private LocalDate entryDate;
    private int entryPrice;
    private LocalDate exitDate5Day;
    private int exitPrice5Day;
    private double return5Day;
    private boolean win5Day;
    private LocalDate exitDate10Day;
    private int exitPrice10Day;
    private double return10Day;
    private boolean win10Day;
}
