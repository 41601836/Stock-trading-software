package com.stock.analysis.module.risk;

import com.stock.analysis.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回测控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestEngine backtestEngine;

    /**
     * 自动复盘
     */
    @GetMapping("/auto-review")
    public Result<BacktestResult> autoReview(
            @RequestParam String stockCode,
            @RequestParam(defaultValue = "2") int years,
            @RequestParam(defaultValue = "85") double minScore) {
        
        try {
            BacktestResult result = backtestEngine.autoReview(stockCode, years, minScore);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to auto review stock {}", stockCode, e);
            return Result.error("自动复盘失败: " + e.getMessage());
        }
    }

    /**
     * 获取历史胜率
     */
    @GetMapping("/win-rate")
    public Result<Double> getWinRate(
            @RequestParam String stockCode,
            @RequestParam(defaultValue = "85") double minScore,
            @RequestParam(defaultValue = "5") int holdingDays) {
        
        try {
            double winRate = backtestEngine.getHistoricalWinRate(stockCode, minScore, holdingDays);
            return Result.success(winRate);
        } catch (Exception e) {
            log.error("Failed to get win rate for stock {}", stockCode, e);
            return Result.error("获取历史胜率失败: " + e.getMessage());
        }
    }

    /**
     * 扫描历史信号
     */
    @GetMapping("/scan-signals")
    public Result<BacktestResult> scanSignals(
            @RequestParam String stockCode,
            @RequestParam(defaultValue = "2") int years,
            @RequestParam(defaultValue = "85") double minScore) {
        
        try {
            BacktestResult result = backtestEngine.autoReview(stockCode, years, minScore);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to scan signals for stock {}", stockCode, e);
            return Result.error("扫描历史信号失败: " + e.getMessage());
        }
    }
}
