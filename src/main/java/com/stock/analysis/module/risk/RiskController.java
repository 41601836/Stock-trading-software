package com.stock.analysis.module.risk;

import com.stock.analysis.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskAnalysisService riskAnalysisService;

    /**
     * 账户风险评估
     * @param positions 当前持仓列表
     */
    @PostMapping("/account/assessment")
    public Result<RiskAssessment> assessAccountRisk(@RequestBody List<Position> positions) {
        RiskAssessment assessment = riskAnalysisService.assessAccountRisk(positions);
        return Result.success(assessment);
    }
}
