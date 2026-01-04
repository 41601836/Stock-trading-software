package com.stock.analysis.module.tech;

import com.stock.analysis.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tech/kline")
@RequiredArgsConstructor
public class TechController {

    private final TechAnalysisService techAnalysisService;

    /**
     * K线形态识别与信号检测
     * @param stockCode 股票代码
     */
    @GetMapping("/pattern/recognize")
    public Result<TechAnalysisService.PatternResult> recognizePattern(@RequestParam String stockCode) {
        TechAnalysisService.PatternResult result = techAnalysisService.recognizePattern(stockCode);
        return Result.success(result);
    }
}
