package com.stock.analysis.module.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position implements Serializable {
    private String stockCode;
    private String stockName;
    private int costPrice;      // 成本价(分)
    private int currentPrice;   // 现价(分)
    private int highestPrice;   // 持仓期间最高价(分) - 用于移动止损
    private int quantity;       // 持仓数量(股)
}
