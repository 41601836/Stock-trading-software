package com.stock.analysis.module.risk.rule;

import com.stock.analysis.module.risk.Position;

/**
 * 移动止损规则 (Trailing Stop)
 */
public class TrailingStopRule implements StopLossRule {

    private final double drawdownThreshold; // 例如 0.08 表示回撤 8%

    /**
     * 默认构造函数，使用8%作为默认阈值
     */
    public TrailingStopRule() {
        this.drawdownThreshold = 0.08;
    }

    public TrailingStopRule(double drawdownThreshold) {
        this.drawdownThreshold = drawdownThreshold;
    }

    @Override
    public boolean check(Position position) {
        if (position.getHighestPrice() <= 0) return false;
        
        // 计算从最高点的回撤幅度
        double drawdown = (double) (position.getHighestPrice() - position.getCurrentPrice()) / position.getHighestPrice();
        return drawdown >= drawdownThreshold;
    }

    @Override
    public String getDescription() {
        return "触发移动止损 (回撤阈值: " + (drawdownThreshold * 100) + "%)";
    }
}
