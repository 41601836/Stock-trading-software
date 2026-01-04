package com.stock.analysis.module.risk;

import com.stock.analysis.module.largeorder.LargeOrderMonitorService;
import com.stock.analysis.module.risk.rule.FixedPercentageRule;
import com.stock.analysis.module.risk.rule.StopLossRule;
import com.stock.analysis.module.risk.rule.TrailingStopRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAnalysisService {

    private final LargeOrderMonitorService largeOrderService;
    
    // 预设规则集 (实际可从数据库加载)
    private final List<StopLossRule> rules = List.of(
            new FixedPercentageRule(0.10), // 10% 固定止损
            new TrailingStopRule(0.08)     // 8% 移动止损
    );

    /**
     * 评估账户风险
     */
    public RiskAssessment assessAccountRisk(List<Position> positions) {
        long totalAsset = positions.stream()
                .mapToLong(p -> (long) p.getCurrentPrice() * p.getQuantity())
                .sum();

        List<String> warnings = new ArrayList<>();
        int healthScore = 100;

        // 1. 规则引擎检查 (止损)
        for (Position pos : positions) {
            for (StopLossRule rule : rules) {
                if (rule.check(pos)) {
                    String msg = String.format("股票[%s] %s", pos.getStockCode(), rule.getDescription());
                    warnings.add(msg);
                    healthScore -= 15; // 触发止损规则扣分
                }
            }
            
            // 2. 联动大单模块：检查主力资金流向
            long netInflow = largeOrderService.getNetInflow(pos.getStockCode(), 3); // 检查近3日
            if (netInflow < -10_000_000) { // 净流出超过 1000万 (分)
                String msg = String.format("股票[%s] 主力大幅出货 (净流出 %.2f 万)，建议规避", 
                        pos.getStockCode(), Math.abs(netInflow) / 1000000.0);
                warnings.add(msg);
                healthScore -= 20; // 主力出货严重扣分
            }
        }

        // 3. VaR 计算 (历史模拟法)
        long var = calculateVaR(totalAsset);
        
        // 如果 VaR 超过总资产的 5%，认为风险过高
        if (totalAsset > 0 && (double) var / totalAsset > 0.05) {
            warnings.add("组合 VaR 过高，建议降低仓位");
            healthScore -= 10;
        }

        if (healthScore < 0) healthScore = 0;

        return RiskAssessment.builder()
                .totalAsset(totalAsset)
                .valueAtRisk(var)
                .healthScore(healthScore)
                .riskWarnings(warnings)
                .suggestion(warnings.isEmpty() ? "持仓健康，继续持有" : "请根据预警信息调整仓位")
                .build();
    }

    /**
     * 计算 VaR (95% 置信度, 1天)
     * 使用历史模拟法 (Historical Simulation)
     */
    private long calculateVaR(long totalAsset) {
        if (totalAsset == 0) return 0;

        // 1. 获取历史收益率序列 (Mock: 模拟过去 250 个交易日的日收益率)
        List<Double> historicalReturns = mockHistoricalReturns(250);

        // 2. 排序
        Collections.sort(historicalReturns);

        // 3. 找到 5% 分位数 (95% 置信度)
        int index = (int) (historicalReturns.size() * 0.05);
        double worstReturn = historicalReturns.get(index); // 这是一个负数，例如 -0.03

        // 4. 计算 VaR (绝对值)
        return (long) Math.abs(totalAsset * worstReturn);
    }

    private List<Double> mockHistoricalReturns(int days) {
        List<Double> returns = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < days; i++) {
            // 模拟正态分布: 均值 0.0005, 标准差 0.02
            double r = random.nextGaussian() * 0.02 + 0.0005;
            returns.add(r);
        }
        return returns;
    }
}
