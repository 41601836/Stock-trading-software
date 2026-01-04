package com.stock.analysis.module.chip;

import com.stock.analysis.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chip")
@RequiredArgsConstructor
public class ChipController {

    private final ChipAnalysisService chipAnalysisService;

    /**
     * 获取筹码分布分析结果
     * @param stockCode 股票代码
     * @return 筹码分布数据
     */
    @GetMapping("/distribution")
    public Result<StockChip> getChipDistribution(@RequestParam String stockCode) {
        StockChip stockChip = chipAnalysisService.calculateDistribution(stockCode);
        return Result.success(stockChip);
    }
}
