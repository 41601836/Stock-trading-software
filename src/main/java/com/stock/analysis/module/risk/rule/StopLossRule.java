package com.stock.analysis.module.risk.rule;

import com.stock.analysis.module.risk.Position;

/**
 * 止损规则接口
 */
public interface StopLossRule {
    
    /**
     * 检查是否触发止损
     * @param position 持仓信息
     * @return 如果触发返回 true
     */
    boolean check(Position position);
    
    /**
     * 获取规则描述
     */
    String getDescription();
}
