package com.stock.analysis.module.risk;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class RiskAssessment implements Serializable {
    
    // 账户总资产(分)
    private long totalAsset;
    
    // VaR (95%置信度下的最大潜在单日亏损, 分)
    private long valueAtRisk;
    
    // 账户健康分 (0-100)
    private int healthScore;
    
    // 触发的风险规则列表
    private List<String> riskWarnings;
    
    // 建议操作 (如: "减仓 600519")
    private String suggestion;
}
