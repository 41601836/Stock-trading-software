package com.stock.analysis;

import com.stock.analysis.module.chip.StockChip;
import com.stock.analysis.module.decision.SwingDecisionEngine;
import com.stock.analysis.module.largeorder.FlinkLargeOrderDetector;
import com.stock.analysis.module.largeorder.LargeOrderEvent;
import com.stock.analysis.module.largeorder.TradeData;
import com.stock.analysis.module.tech.DTWUtil;
import com.stock.analysis.module.tech.Kline;
import com.stock.analysis.module.tech.KlinePatternRecognizer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 性能测试类
 * 验证实现的功能是否满足性能要求（实时<300ms，非实时<1000ms）
 */
public class PerformanceTest {

    private static final int TEST_COUNT = 100;
    private static final Random random = new Random();

    /**
     * 测试Flink大单检测的动态阈值计算性能
     */
    @Test
    public void testDynamicThresholdCalculationPerformance() {
        System.out.println("=== 测试Flink大单检测的动态阈值计算性能 ===");

        // 准备测试数据
        List<TradeData> tradeDataList = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            TradeData trade = generateRandomTradeData();
            tradeDataList.add(trade);
        }

        // 测试多次，取平均值
        long totalTime = 0;
        for (int i = 0; i < TEST_COUNT; i++) {
            long startTime = System.nanoTime();
            
            // 调用意图识别方法（这是性能关键部分）
            long amount = (long) (10000000 + random.nextDouble() * 1000000000);
            long threshold = (long) (50000000 + random.nextDouble() * 500000000);
            String direction = random.nextBoolean() ? "BUY" : "SELL";
            int price = 1000 + random.nextInt(200);
            double priceTrend = (random.nextDouble() - 0.5) * 0.4;
            
            // 调用意图识别方法（这是性能关键部分）
            FlinkLargeOrderDetector.identifyIntent(amount, threshold, direction, price, priceTrend, 
                    tradeDataList.get(random.nextInt(tradeDataList.size())), 
                    tradeDataList.toArray(new TradeData[0]));
            
            // 计算超过阈值的比例（用于后续可能的分析）
            double exceedRatio = (double) amount / threshold;
            
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }

        double avgTime = totalTime / (double) TEST_COUNT / 1000000; // 转换为毫秒
        System.out.printf("动态阈值计算和意图识别平均耗时: %.2f ms\n", avgTime);
        System.out.printf("是否满足实时要求(<300ms): %b\n\n", avgTime < 300);
    }

    /**
     * 测试SwingDecisionEngine的胜率评分计算性能
     */
    @Test
    public void testSwingDecisionEnginePerformance() {
        System.out.println("=== 测试SwingDecisionEngine的胜率评分计算性能 ===");

        SwingDecisionEngine decisionEngine = new SwingDecisionEngine();
        String stockCode = "SH600000";
        
        // 准备测试数据
        StockChip stockChip = generateRandomStockChip();
        List<LargeOrderEvent> largeOrders = generateRandomLargeOrders();

        // 测试多次，取平均值
        long totalTime = 0;
        for (int i = 0; i < TEST_COUNT; i++) {
            long startTime = System.nanoTime();
            
            // 调用胜率评分计算
            int score = decisionEngine.calculateWinRateScore(stockCode, stockChip, largeOrders);
            
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }

        double avgTime = totalTime / (double) TEST_COUNT / 1000000; // 转换为毫秒
        System.out.printf("胜率评分计算平均耗时: %.2f ms\n", avgTime);
        System.out.printf("是否满足实时要求(<300ms): %b\n\n", avgTime < 300);
    }

    /**
     * 测试DTW算法的性能
     */
    @Test
    public void testDTWPerformance() {
        System.out.println("=== 测试DTW算法的性能 ===");

        // 准备测试数据
        List<Double> sequence1 = generateRandomSequence(100);
        List<Double> sequence2 = generateRandomSequence(100);

        // 测试多次，取平均值
        long totalTime = 0;
        for (int i = 0; i < TEST_COUNT; i++) {
            long startTime = System.nanoTime();
            
            // 调用DTW计算
            double distance = DTWUtil.calculateDTW(sequence1, sequence2);
            
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }

        double avgTime = totalTime / (double) TEST_COUNT / 1000000; // 转换为毫秒
        System.out.printf("DTW计算平均耗时: %.2f ms\n", avgTime);
        System.out.printf("是否满足非实时要求(<1000ms): %b\n\n", avgTime < 1000);
    }

    /**
     * 测试K线形态识别的性能
     */
    @Test
    public void testKlinePatternRecognitionPerformance() {
        System.out.println("=== 测试K线形态识别的性能 ===");

        KlinePatternRecognizer recognizer = new KlinePatternRecognizer();
        
        // 准备测试数据
        List<Kline> klines = generateRandomKlines(50);

        // 测试多次，取平均值
        long totalTime = 0;
        for (int i = 0; i < TEST_COUNT; i++) {
            long startTime = System.nanoTime();
            
            // 调用形态识别
            KlinePatternRecognizer.PatternRecognitionResult result = recognizer.recognizePattern(klines);
            
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }

        double avgTime = totalTime / (double) TEST_COUNT / 1000000; // 转换为毫秒
        System.out.printf("K线形态识别平均耗时: %.2f ms\n", avgTime);
        System.out.printf("是否满足非实时要求(<1000ms): %b\n\n", avgTime < 1000);
    }

    /**
     * 生成随机交易数据
     */
    private TradeData generateRandomTradeData() {
        String stockCode = "SH600000";
        int price = 1000 + random.nextInt(200);
        long amount = 1000000L + random.nextLong(100000000L);
        String direction = random.nextBoolean() ? "BUY" : "SELL";
        String[] seatNames = {"机构专用", "朱雀大街", "南京唯宁路", "普通席位", "散户席位"};
        String[] traderTypes = {"机构", "游资", "游资", "散户", "散户"};
        int seatIndex = random.nextInt(seatNames.length);
        String tradeTime = java.time.LocalDateTime.now().toString();

        return new TradeData(stockCode, price, amount, direction, 
                seatNames[seatIndex], traderTypes[seatIndex], tradeTime);
    }

    /**
     * 生成随机股票筹码数据
     */
    private StockChip generateRandomStockChip() {
        StockChip stockChip = new StockChip();
        stockChip.setIsSinglePeak(true);
        stockChip.setIsLowPosition(true);
        return stockChip;
    }

    /**
     * 生成随机大单事件列表
     */
    private List<LargeOrderEvent> generateRandomLargeOrders() {
        List<LargeOrderEvent> largeOrders = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            LargeOrderEvent event = new LargeOrderEvent(
                    this,
                    "SH600000",
                    100000000L + random.nextLong(900000000L),
                    1000 + random.nextInt(200),
                    "BUY",
                    i % 5 == 0 ? "三家机构大买" : "机构扫货"
            );
            largeOrders.add(event);
        }
        return largeOrders;
    }

    /**
     * 生成随机序列
     */
    private List<Double> generateRandomSequence(int length) {
        List<Double> sequence = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            sequence.add(1000 + random.nextDouble() * 200);
        }
        return sequence;
    }

    /**
     * 生成随机K线数据
     */
    private List<Kline> generateRandomKlines(int count) {
        List<Kline> klines = new ArrayList<>();
        int price = 1000;
        for (int i = 0; i < count; i++) {
            Kline kline = new Kline();
            kline.setOpen(price);
            kline.setHigh(price + random.nextInt(20));
            kline.setLow(price - random.nextInt(20));
            price = kline.getLow() + random.nextInt(kline.getHigh() - kline.getLow() + 1);
            kline.setClose(price);
            kline.setVolume(1000000L + random.nextLong(90000000L));
            klines.add(kline);
        }
        return klines;
    }
}
