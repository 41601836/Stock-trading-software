package com.stock.analysis.module.risk;

import com.stock.analysis.module.chip.ChipAnalysisService;
import com.stock.analysis.module.chip.StockChip;
import com.stock.analysis.module.decision.SwingDecisionEngine;
import com.stock.analysis.module.largeorder.LargeOrderEvent;
import com.stock.analysis.module.market.MarketDataService;
import com.stock.analysis.module.market.MarketDataService.KlinePeriod;
import com.stock.analysis.module.tech.Kline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 回测引擎实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestEngineImpl implements BacktestEngine {

    private final MarketDataService marketDataService;
    private final ChipAnalysisService chipAnalysisService;
    private final SwingDecisionEngine swingDecisionEngine;
    private final Random random = new Random();

    @Override
    public List<BacktestSignal> scanHistoricalSignals(String stockCode, int years, double minScore) {
        // 计算时间范围
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(years);

        // 获取历史K线数据
        List<Kline> klines = marketDataService.getHistoricalKlines(stockCode, startDate, endDate, KlinePeriod.DAY);
        List<BacktestSignal> signals = new ArrayList<>();

        // 使用真实评分模型计算评分
        for (Kline kline : klines) {
            // 获取历史筹码分布数据
            StockChip stockChip = getHistoricalStockChip(stockCode, kline);
            
            // 获取历史大单事件数据
            List<LargeOrderEvent> largeOrders = getHistoricalLargeOrders(stockCode, kline);
            
            // 使用决策引擎计算真实评分
            int score = swingDecisionEngine.calculateWinRateScore(stockCode, stockChip, largeOrders);

            // 筛选出满足评分条件的信号
            if (score >= minScore) {
                BacktestSignal signal = BacktestSignal.builder()
                        .stockCode(stockCode)
                        .signalDate(kline.getDate())
                        .score(score)
                        .signalPrice(kline.getClose())
                        .build();
                signals.add(signal);
            }
        }

        log.info("Found {} signals for stock {} from {} to {} with score >= {}", 
                signals.size(), stockCode, startDate, endDate, minScore);
        return signals;
    }
    
    /**
     * 获取历史筹码分布数据 (模拟实现)
     */
    private StockChip getHistoricalStockChip(String stockCode, Kline kline) {
        // 调用当前筹码分析服务获取筹码分布
        StockChip currentChip = chipAnalysisService.calculateDistribution(stockCode);
        
        // 根据K线数据调整筹码分布参数，模拟历史筹码状态
        int mainCost = kline.getClose() + (random.nextInt(100) - 50); // 主筹成本在收盘价附近
        boolean isLowPosition = (double) kline.getClose() / mainCost < 1.05; // 当前价格低于主筹成本5%
        
        // 随机模拟低位单峰密集情况 (约10%概率)
        boolean isSinglePeak = random.nextDouble() < 0.1;
        boolean isSinglePeakLow = isSinglePeak && isLowPosition;
        
        // 返回调整后的历史筹码数据
        return StockChip.builder()
                .stockCode(stockCode)
                .analyzeTime(LocalDateTime.now())
                .currentPrice(kline.getClose())
                .mainCost(mainCost)
                .isSinglePeak(isSinglePeak)
                .isLowPosition(isLowPosition)
                .isSinglePeakLow(isSinglePeakLow)
                .detailedDistribution(currentChip.getDetailedDistribution())
                .build();
    }
    
    /**
     * 获取历史大单事件数据 (模拟实现)
     */
    private List<LargeOrderEvent> getHistoricalLargeOrders(String stockCode, Kline kline) {
        List<LargeOrderEvent> largeOrders = new ArrayList<>();
        
        // 随机模拟三家机构大买事件 (约5%概率)
        if (random.nextDouble() < 0.05) {
            LargeOrderEvent event = new LargeOrderEvent(
                    this,
                    stockCode,
                    100000000L, // 1亿
                    kline.getClose(),
                    "BUY",
                    "三家机构大买"
            );
            largeOrders.add(event);
        }
        
        return largeOrders;
    }

    @Override
    public BacktestResult runBacktest(String stockCode, List<BacktestSignal> signals, List<Integer> holdingDaysList) {
        if (signals.isEmpty()) {
            return BacktestResult.builder()
                    .stockCode(stockCode)
                    .totalSignals(0)
                    .winRate5Day(0.0)
                    .profitLossRatio5Day(0.0)
                    .winRate10Day(0.0)
                    .profitLossRatio10Day(0.0)
                    .build();
        }

        // 获取所有信号日期的范围
        LocalDate minDate = signals.stream().map(BacktestSignal::getSignalDate).min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate maxDate = signals.stream().map(BacktestSignal::getSignalDate).max(LocalDate::compareTo).orElse(LocalDate.now());
        
        // 获取完整的K线数据范围（包含所有信号日期之后的最大持股天数）
        int maxHoldingDays = holdingDaysList.stream().max(Integer::compareTo).orElse(10);
        LocalDate endDate = maxDate.plusDays(maxHoldingDays);
        
        // 获取历史K线数据
        List<Kline> klines = marketDataService.getHistoricalKlines(stockCode, minDate, endDate, KlinePeriod.DAY);
        
        // 将K线数据按日期分组，便于快速查找
        Map<LocalDate, Kline> klineMap = klines.stream()
                .collect(Collectors.toMap(Kline::getDate, kline -> kline));

        // 执行回测，计算每个信号的交易结果
        List<TradeResult> tradeResults = new ArrayList<>();
        for (BacktestSignal signal : signals) {
            LocalDate entryDate = signal.getSignalDate();
            int entryPrice = signal.getSignalPrice();
            
            // 计算5天和10天的交易结果
            TradeResult.TradeResultBuilder builder = TradeResult.builder()
                    .entryDate(entryDate)
                    .entryPrice(entryPrice);

            // 计算5天持股结果
            LocalDate exitDate5Day = entryDate.plusDays(5);
            if (klineMap.containsKey(exitDate5Day)) {
                int exitPrice5Day = klineMap.get(exitDate5Day).getClose();
                double return5Day = (double) (exitPrice5Day - entryPrice) / entryPrice;
                boolean win5Day = return5Day > 0;
                
                builder.exitDate5Day(exitDate5Day)
                        .exitPrice5Day(exitPrice5Day)
                        .return5Day(return5Day)
                        .win5Day(win5Day);
            }

            // 计算10天持股结果
            LocalDate exitDate10Day = entryDate.plusDays(10);
            if (klineMap.containsKey(exitDate10Day)) {
                int exitPrice10Day = klineMap.get(exitDate10Day).getClose();
                double return10Day = (double) (exitPrice10Day - entryPrice) / entryPrice;
                boolean win10Day = return10Day > 0;
                
                builder.exitDate10Day(exitDate10Day)
                        .exitPrice10Day(exitPrice10Day)
                        .return10Day(return10Day)
                        .win10Day(win10Day);
            }

            tradeResults.add(builder.build());
        }

        // 计算胜率和盈亏比
        double winRate5Day = calculateWinRate(tradeResults, true);
        double profitLossRatio5Day = calculateProfitLossRatio(tradeResults, true);
        double winRate10Day = calculateWinRate(tradeResults, false);
        double profitLossRatio10Day = calculateProfitLossRatio(tradeResults, false);

        // 构建回测结果
        return BacktestResult.builder()
                .stockCode(stockCode)
                .startDate(minDate)
                .endDate(maxDate)
                .totalSignals(signals.size())
                .tradeResults(tradeResults)
                .winRate5Day(winRate5Day)
                .profitLossRatio5Day(profitLossRatio5Day)
                .winRate10Day(winRate10Day)
                .profitLossRatio10Day(profitLossRatio10Day)
                .build();
    }

    @Override
    public double getHistoricalWinRate(String stockCode, double minScore, int holdingDays) {
        // 扫描历史信号
        List<BacktestSignal> signals = scanHistoricalSignals(stockCode, 2, minScore);
        
        if (signals.isEmpty()) {
            return 0.0;
        }

        // 执行回测
        List<Integer> holdingDaysList = List.of(holdingDays);
        BacktestResult result = runBacktest(stockCode, signals, holdingDaysList);

        // 返回指定持股天数的胜率
        if (holdingDays == 5) {
            return result.getWinRate5Day();
        } else if (holdingDays == 10) {
            return result.getWinRate10Day();
        }

        return 0.0;
    }

    @Override
    public BacktestResult autoReview(String stockCode, int years, double minScore) {
        // 扫描历史信号
        List<BacktestSignal> signals = scanHistoricalSignals(stockCode, years, minScore);
        
        // 执行回测
        List<Integer> holdingDaysList = List.of(5, 10);
        return runBacktest(stockCode, signals, holdingDaysList);
    }

    /**
     * 计算胜率
     */
    private double calculateWinRate(List<TradeResult> tradeResults, boolean is5Day) {
        if (tradeResults.isEmpty()) {
            return 0.0;
        }

        long totalTrades = tradeResults.stream()
                .filter(result -> is5Day ? result.getExitDate5Day() != null : result.getExitDate10Day() != null)
                .count();

        if (totalTrades == 0) {
            return 0.0;
        }

        long winningTrades = tradeResults.stream()
                .filter(result -> is5Day ? result.isWin5Day() : result.isWin10Day())
                .count();

        return (double) winningTrades / totalTrades * 100;
    }

    /**
     * 计算盈亏比
     */
    private double calculateProfitLossRatio(List<TradeResult> tradeResults, boolean is5Day) {
        if (tradeResults.isEmpty()) {
            return 0.0;
        }

        // 计算总盈利和总亏损
        double totalProfit = 0.0;
        double totalLoss = 0.0;
        long winningTrades = 0;
        long losingTrades = 0;

        for (TradeResult result : tradeResults) {
            double returnRate;
            if (is5Day) {
                if (result.getExitDate5Day() == null) continue;
                returnRate = result.getReturn5Day();
            } else {
                if (result.getExitDate10Day() == null) continue;
                returnRate = result.getReturn10Day();
            }

            if (returnRate > 0) {
                totalProfit += returnRate;
                winningTrades++;
            } else if (returnRate < 0) {
                totalLoss += Math.abs(returnRate);
                losingTrades++;
            }
        }

        if (winningTrades == 0 || losingTrades == 0) {
            return 0.0;
        }

        // 计算平均盈利和平均亏损
        double avgProfit = totalProfit / winningTrades;
        double avgLoss = totalLoss / losingTrades;

        // 计算盈亏比
        return avgProfit / avgLoss;
    }
}
