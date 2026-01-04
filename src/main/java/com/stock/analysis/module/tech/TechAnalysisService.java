package com.stock.analysis.module.tech;

import com.stock.analysis.module.chip.ChipAnalysisService;
import com.stock.analysis.module.chip.StockChip;
import com.stock.analysis.module.largeorder.LargeOrderMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TechAnalysisService {

    private final ChipAnalysisService chipAnalysisService;
    private final LargeOrderMonitorService largeOrderService;

    /**
     * 识别 K 线形态并判断是否触发“三剑合一”信号
     */
    public PatternResult recognizePattern(String stockCode) {
        // 1. 获取 K 线数据 (Mock)
        List<Kline> klines = mockKlines(stockCode, 60);
        
        // 2. 计算技术指标
        List<Integer> ma20 = IndicatorCalculator.calculateMA(klines, 20);
        List<Double> rsi14 = IndicatorCalculator.calculateRSI(klines, 14);
        
        // 3. DTW 模式匹配 (对比标准暴涨模型)
        List<Double> currentSequence = normalize(klines.stream().map(k -> (double) k.getClose()).toList());
        List<Double> rocketModel = getRocketModel(); // 获取标准模型
        double dtwDistance = DTWUtil.calculateDTW(currentSequence, rocketModel);
        
        boolean isPatternMatch = dtwDistance < 5.0; // 假设阈值为 5.0

        // 4. 三剑合一逻辑判定
        // 4.1 筹码单峰密集
        StockChip chipData = chipAnalysisService.calculateDistribution(stockCode);
        boolean isChipDense = chipData.getConcentrationRatio().compareTo(new BigDecimal("0.15")) < 0; // < 15%

        // 4.2 大单持续流入
        long netInflow = largeOrderService.getNetInflow(stockCode, 5);
        boolean isLargeOrderInflow = netInflow > 0;

        // 4.3 K线突破平台 (当前价格 > 20日均线 && 形态匹配)
        int currentPrice = klines.get(klines.size() - 1).getClose();
        int lastMa20 = ma20.get(ma20.size() - 1);
        boolean isBreakout = currentPrice > lastMa20 && isPatternMatch;

        boolean isThreeSwordsUnited = isChipDense && isLargeOrderInflow && isBreakout;

        return PatternResult.builder()
                .stockCode(stockCode)
                .dtwDistance(dtwDistance)
                .isChipDense(isChipDense)
                .isLargeOrderInflow(isLargeOrderInflow)
                .isBreakout(isBreakout)
                .isThreeSwordsUnited(isThreeSwordsUnited)
                .signalMsg(isThreeSwordsUnited ? "【三剑合一】高价值买入信号触发！" : "未触发信号")
                .build();
    }

    // ----------------- 辅助方法 -----------------

    private List<Double> normalize(List<Double> input) {
        // 简单归一化到 0-1
        double min = input.stream().min(Double::compare).orElse(0.0);
        double max = input.stream().max(Double::compare).orElse(1.0);
        if (max == min) return input;
        return input.stream().map(v -> (v - min) / (max - min)).collect(Collectors.toList());
    }

    private List<Double> getRocketModel() {
        // 模拟一个“横盘后拉升”的标准模型 (归一化后)
        // 前段平稳，后段急剧上升
        List<Double> model = new ArrayList<>();
        for (int i = 0; i < 50; i++) model.add(0.2 + new Random().nextDouble() * 0.1); // 0.2-0.3 震荡
        for (int i = 0; i < 10; i++) model.add(0.3 + i * 0.07); // 拉升到 1.0
        return model;
    }

    private List<Kline> mockKlines(String stockCode, int days) {
        List<Kline> list = new ArrayList<>();
        LocalDate end = LocalDate.now();
        Random r = new Random();
        int price = 1000;
        for (int i = days; i >= 0; i--) {
            int change = r.nextInt(100) - 40; // -40 ~ +60 (微涨趋势)
            price += change;
            if (price < 100) price = 100;
            list.add(Kline.builder()
                    .date(end.minusDays(i))
                    .close(price)
                    .open(price - change + r.nextInt(20))
                    .high(price + r.nextInt(30))
                    .low(price - r.nextInt(30))
                    .volume(1000000 + r.nextInt(5000000))
                    .build());
        }
        return list;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class PatternResult {
        private String stockCode;
        private double dtwDistance;
        private boolean isChipDense;
        private boolean isLargeOrderInflow;
        private boolean isBreakout;
        private boolean isThreeSwordsUnited;
        private String signalMsg;
    }
}
