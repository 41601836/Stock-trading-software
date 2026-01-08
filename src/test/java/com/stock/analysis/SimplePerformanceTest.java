package com.stock.analysis;

import com.stock.analysis.module.chip.ChipAnalysisServiceImpl;
import com.stock.analysis.module.largeorder.FlinkLargeOrderDetector;
import com.stock.analysis.module.largeorder.LargeOrderEvent;
import com.stock.analysis.module.tech.DTWUtil;
import com.stock.analysis.module.tech.Kline;
import com.stock.analysis.module.tech.KlinePatternRecognizer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 简化的性能测试类，直接运行核心功能测试
 */
public class SimplePerformanceTest {

    private static final int TEST_ITERATIONS = 1000;
    private static final Random random = new Random();

    public static void main(String[] args) {
        System.out.println("开始简化版性能测试...");
        
        // 测试WAD模型性能
        testWADPerformance();
        
        // 测试Flink动态阈值计算性能
        testDynamicThresholdCalculation();
        
        // 测试交易意图识别性能
        testIntentRecognition();
        
        // 测试K线形态识别性能
        testPatternRecognition();
        
        // 测试DTW序列匹配性能
        testDTWPerformance();
        
        System.out.println("所有性能测试完成！");
    }

    /**
     * 测试WAD模型性能
     */
    private static void testWADPerformance() {
        System.out.println("\n=== 测试WAD模型性能 ===");
        
        // 创建ChipAnalysisServiceImpl实例 - 注意：实际应用中需要注入依赖
        // 这里使用模拟的构造函数调用
        ChipAnalysisServiceImpl wadService = null;
        try {
            // 使用反射创建实例，避免依赖注入问题
            wadService = ChipAnalysisServiceImpl.class.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("无法创建ChipAnalysisServiceImpl实例: " + e.getMessage());
            return;
        }
        
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            wadService.calculateDistribution("SH600000");
        }
        long endTime = System.nanoTime();
        
        double averageTimeMs = (endTime - startTime) / (double) TEST_ITERATIONS / 1_000_000;
        System.out.println("WAD模型平均计算时间: " + averageTimeMs + " ms");
        System.out.println("满足要求: " + (averageTimeMs < 1000 ? "是" : "否") + " (<= 1000ms)");
    }

    /**
     * 测试Flink动态阈值计算性能
     */
    private static void testDynamicThresholdCalculation() {
        System.out.println("\n=== 测试Flink动态阈值计算性能 ===");
        
        // 创建模拟数据
        List<com.stock.analysis.module.largeorder.TradeData> tradeDataList = generateFlinkTestTradeData(2000);
        
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            // 模拟动态阈值计算
            int count = tradeDataList.size();
            double[] amounts = new double[count];
            for (int j = 0; j < count; j++) {
                amounts[j] = tradeDataList.get(j).getAmount();
            }
            // 简单计算中位数和标准差
            double median = calculateMedian(amounts);
            double std = calculateStandardDeviation(amounts);
            double threshold = median + 2.5 * std;
        }
        long endTime = System.nanoTime();
        
        double averageTimeMs = (endTime - startTime) / (double) TEST_ITERATIONS / 1_000_000;
        System.out.println("动态阈值计算平均时间: " + averageTimeMs + " ms");
        System.out.println("满足要求: " + (averageTimeMs < 300 ? "是" : "否") + " (<= 300ms)");
    }

    /**
     * 测试交易意图识别性能
     */
    private static void testIntentRecognition() {
        System.out.println("\n=== 测试交易意图识别性能 ===");
        
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long amount = 100000000L + random.nextLong(900000000L); // 100万到1亿
            long threshold = 50000000L;
            String direction = random.nextBoolean() ? "BUY" : "SELL";
            int price = 1000 + random.nextInt(200);
            double priceTrend = -0.5 + random.nextDouble();
            
            // 模拟交易数据
            com.stock.analysis.module.largeorder.TradeData currentTrade = new com.stock.analysis.module.largeorder.TradeData(
                    "SH600000", price, amount, direction, "机构专用", "机构", LocalDateTime.now().toString()
            );
            
            com.stock.analysis.module.largeorder.TradeData[] recentTrades = generateFlinkTestTradeData(10).toArray(new com.stock.analysis.module.largeorder.TradeData[0]);
            
            // 调用意图识别
            String intent = FlinkLargeOrderDetector.identifyIntent(amount, threshold, direction, price, priceTrend, currentTrade, recentTrades);
        }
        long endTime = System.nanoTime();
        
        double averageTimeMs = (endTime - startTime) / (double) TEST_ITERATIONS / 1_000_000;
        System.out.println("交易意图识别平均时间: " + averageTimeMs + " ms");
        System.out.println("满足要求: " + (averageTimeMs < 100 ? "是" : "否") + " (<= 100ms)");
    }

    /**
     * 测试K线形态识别性能
     */
    private static void testPatternRecognition() {
        System.out.println("\n=== 测试K线形态识别性能 ===");
        
        KlinePatternRecognizer recognizer = new KlinePatternRecognizer();
        List<Kline> klines = generateTestKlines(50);
        
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            recognizer.recognizePattern(klines);
        }
        long endTime = System.nanoTime();
        
        double averageTimeMs = (endTime - startTime) / (double) TEST_ITERATIONS / 1_000_000;
        System.out.println("K线形态识别平均时间: " + averageTimeMs + " ms");
        System.out.println("满足要求: " + (averageTimeMs < 500 ? "是" : "否") + " (<= 500ms)");
    }

    /**
     * 测试DTW序列匹配性能
     */
    private static void testDTWPerformance() {
        System.out.println("\n=== 测试DTW序列匹配性能 ===");
        
        List<Double> sequence1 = generateTestSequence(100);
        List<Double> sequence2 = generateTestSequence(100);
        
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            double distance = DTWUtil.calculateDTW(sequence1, sequence2);
        }
        long endTime = System.nanoTime();
        
        double averageTimeMs = (endTime - startTime) / (double) TEST_ITERATIONS / 1_000_000;
        System.out.println("DTW序列匹配平均时间: " + averageTimeMs + " ms");
        System.out.println("满足要求: " + (averageTimeMs < 800 ? "是" : "否") + " (<= 800ms)");
    }



    /**
     * 生成Flink测试用的交易数据
     */
    private static List<com.stock.analysis.module.largeorder.TradeData> generateFlinkTestTradeData(int count) {
        List<com.stock.analysis.module.largeorder.TradeData> tradeDataList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < count; i++) {
            int price = 1000 + random.nextInt(200);
            long amount = 100000L + random.nextLong(100000000L);
            String direction = random.nextBoolean() ? "BUY" : "SELL";
            String seatName = random.nextDouble() > 0.3 ? "机构专用" : "普通席位";
            String traderType = seatName.equals("机构专用") ? "机构" : "散户";
            
            com.stock.analysis.module.largeorder.TradeData trade = new com.stock.analysis.module.largeorder.TradeData(
                    "SH600000", price, amount, direction, seatName, traderType, now.minusMinutes(count - i).toString()
            );
            tradeDataList.add(trade);
        }
        
        return tradeDataList;
    }

    /**
     * 生成测试用的K线数据
     */
    private static List<Kline> generateTestKlines(int count) {
        List<Kline> klines = new ArrayList<>();
        int basePrice = 1000;
        
        for (int i = 0; i < count; i++) {
            int open = basePrice + random.nextInt(50);
            int high = open + random.nextInt(30);
            int low = open - random.nextInt(30);
            int close = low + random.nextInt(high - low + 1);
            long volume = 1000000L + random.nextLong(90000000L);
            
            Kline kline = new Kline();
            kline.setOpen(open);
            kline.setHigh(high);
            kline.setLow(low);
            kline.setClose(close);
            kline.setVolume(volume);
            klines.add(kline);
            
            basePrice = close;
        }
        
        return klines;
    }

    /**
     * 生成测试用的序列数据
     */
    private static List<Double> generateTestSequence(int length) {
        List<Double> sequence = new ArrayList<>();
        double value = 100.0;
        
        for (int i = 0; i < length; i++) {
            value += (random.nextDouble() - 0.5) * 10.0;
            sequence.add(value);
        }
        
        return sequence;
    }

    /**
     * 计算中位数
     */
    private static double calculateMedian(double[] array) {
        double[] sorted = array.clone();
        java.util.Arrays.sort(sorted);
        int mid = sorted.length / 2;
        return sorted.length % 2 == 0 ? (sorted[mid - 1] + sorted[mid]) / 2 : sorted[mid];
    }

    /**
     * 计算标准差
     */
    private static double calculateStandardDeviation(double[] array) {
        double mean = 0.0;
        for (double d : array) {
            mean += d;
        }
        mean /= array.length;
        
        double variance = 0.0;
        for (double d : array) {
            variance += Math.pow(d - mean, 2);
        }
        variance /= array.length;
        
        return Math.sqrt(variance);
    }
}
