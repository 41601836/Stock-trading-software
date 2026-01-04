package com.stock.analysis.module.risk.rule;

import com.stock.analysis.module.risk.Position;
import lombok.AllArgsConstructor;

/**
 * 固定比例止损规则
 */
@AllArgsConstructor
public class FixedPercentageRule implements StopLossRule {

    private final double threshold; // 例如 0.10 表示 10%

    @Override
    public boolean check(Position position) {
        if (position.getCostPrice() <= 0) return false;
        
        double lossRate = (double) (position.getCostPrice() - position.getCurrentPrice()) / position.getCostPrice();
        return lossRate >= threshold;
    }

    @Override
    public String getDescription() {
        return "触发固定比例止损 (阈值: " + (threshold * 100) + "%)";
    }
}
