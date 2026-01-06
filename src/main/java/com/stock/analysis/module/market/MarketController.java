package com.stock.analysis.module.market;

import com.stock.analysis.common.result.Result;
import com.stock.analysis.module.tech.Kline;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketDataService marketDataService;

    /**
     * 获取单个股票的实时行情
     * @param stockCode 股票代码
     * @return 实时行情数据
     */
    @GetMapping("/quote/{stockCode}")
    public Result<RealTimeQuote> getRealTimeQuote(@PathVariable String stockCode) {
        RealTimeQuote quote = marketDataService.getRealTimeQuote(stockCode);
        return Result.success(quote);
    }

    /**
     * 获取多个股票的实时行情
     * @param stockCodes 股票代码列表，用逗号分隔
     * @return 实时行情数据列表
     */
    @GetMapping("/quotes")
    public Result<List<RealTimeQuote>> getBatchRealTimeQuotes(@RequestParam String stockCodes) {
        if (stockCodes == null || stockCodes.trim().isEmpty()) {
            return Result.success(List.of());
        }

        List<String> stockCodeList = List.of(stockCodes.split(","));
        List<RealTimeQuote> quotes = marketDataService.getBatchRealTimeQuotes(stockCodeList);
        return Result.success(quotes);
    }

    /**
     * 获取历史K线数据
     * @param stockCode 股票代码
     * @param startDate 开始日期，格式：yyyy-MM-dd
     * @param endDate 结束日期，格式：yyyy-MM-dd
     * @param period K线周期，可选值：DAY, WEEK, MONTH
     * @return K线数据列表
     */
    @GetMapping("/klines/{stockCode}")
    public Result<List<Kline>> getHistoricalKlines(
            @PathVariable String stockCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String period) {

        MarketDataService.KlinePeriod klinePeriod = MarketDataService.KlinePeriod.valueOf(period.toUpperCase());
        List<Kline> klines = marketDataService.getHistoricalKlines(stockCode, startDate, endDate, klinePeriod);
        return Result.success(klines);
    }
}