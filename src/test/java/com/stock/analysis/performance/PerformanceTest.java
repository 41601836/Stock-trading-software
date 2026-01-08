package com.stock.analysis.performance;

import com.stock.analysis.module.market.MarketDataService;
import com.stock.analysis.module.market.RealTimeQuote;
import com.stock.analysis.module.chip.ChipAnalysisService;
import com.stock.analysis.module.chip.StockChip;
import com.stock.analysis.module.tech.Kline;
import com.stock.analysis.module.tech.TechAnalysisService;
import com.stock.analysis.module.tech.TechAnalysisService.PatternResult;
import com.stock.analysis.module.tech.DTWSequenceMatcher;
import com.stock.analysis.module.tech.IndicatorCalculator;
import com.stock.analysis.module.largeorder.FlinkLargeOrderDetector;
import com.stock.analysis.module.largeorder.LargeOrderResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootTest
class PerformanceTest {

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private TechAnalysisService techAnalysisService;
    
    @Autowired
    private ChipAnalysisService chipAnalysisService;

    @Autowired
    private FlinkLargeOrderDetector largeOrderDetector;

    // 线程池配置
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    // 测试配置
    private static final int NUM_REQUESTS = 100;
    private static final String TEST_STOCK_CODE = "600519";
    private static final List<String> TEST_STOCK_CODES = List.of("600519", "000001", "000858", "601318", "600036");

    @Test
    void testRealTimeQuotePerformance() throws InterruptedException, ExecutionException {
        System.out.println("=== 测试实时行情获取性能 ===");
        
        List<Callable<RealTimeQuote>> tasks = IntStream.range(0, NUM_REQUESTS)
                .mapToObj(i -> (Callable<RealTimeQuote>) () -> marketDataService.getRealTimeQuote(TEST_STOCK_CODE))
                .collect(Collectors.toList());

        long startTime = System.currentTimeMillis();
        List<Future<RealTimeQuote>> futures = EXECUTOR.invokeAll(tasks);
        
        // 等待所有任务完成
        int successfulRequests = 0;
        for (Future<RealTimeQuote> future : futures) {
            if (future.get() != null) {
                successfulRequests++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTimePerRequest = (double) totalTime / NUM_REQUESTS;
        
        System.out.println("总请求数: " + NUM_REQUESTS);
        System.out.println("成功请求数: " + successfulRequests);
        System.out.println("总耗时: " + totalTime + " ms");
        System.out.println("平均响应时间: " + avgTimePerRequest + " ms");
        System.out.println("吞吐量: " + (NUM_REQUESTS * 1000.0 / totalTime) + " requests/s");
        System.out.println();
    }
    
    @Test
    void testWadModelPerformance() throws InterruptedException, ExecutionException {
        System.out.println("=== 测试WAD模型计算性能 ===");
        
        // 测试单个请求的性能
        long singleStartTime = System.currentTimeMillis();
        StockChip chip = chipAnalysisService.calculateDistribution(TEST_STOCK_CODE);
        long singleEndTime = System.currentTimeMillis();
        long singleDuration = singleEndTime - singleStartTime;
        System.out.println("单个WAD模型计算耗时: " + singleDuration + " ms");
        
        // 测试并发请求的性能
        List<Callable<StockChip>> tasks = IntStream.range(0, NUM_REQUESTS)
                .mapToObj(i -> (Callable<StockChip>) () -> chipAnalysisService.calculateDistribution(TEST_STOCK_CODE))
                .collect(Collectors.toList());

        long startTime = System.currentTimeMillis();
        List<Future<StockChip>> futures = EXECUTOR.invokeAll(tasks);
        
        // 等待所有任务完成
        int successfulRequests = 0;
        for (Future<StockChip> future : futures) {
            if (future.get() != null) {
                successfulRequests++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTimePerRequest = (double) totalTime / NUM_REQUESTS;
        double throughput = NUM_REQUESTS * 1000.0 / totalTime;
        
        System.out.println("总请求数: " + NUM_REQUESTS);
        System.out.println("成功请求数: " + successfulRequests);
        System.out.println("总耗时: " + totalTime + " ms");
        System.out.println("平均响应时间: " + avgTimePerRequest + " ms");
        System.out.println("吞吐量: " + throughput + " requests/s");
        System.out.println();
    }

    @Test
    void testHistoricalKlinesPerformance() throws InterruptedException, ExecutionException {
        System.out.println("=== 测试历史K线获取性能 ===");
        
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        
        List<Callable<List<Kline>>> tasks = IntStream.range(0, NUM_REQUESTS)
                .mapToObj(i -> (Callable<List<Kline>>) () -> marketDataService.getHistoricalKlines(
                        TEST_STOCK_CODE, startDate, endDate, MarketDataService.KlinePeriod.DAY))
                .collect(Collectors.toList());

        long startTime = System.currentTimeMillis();
        List<Future<List<Kline>>> futures = EXECUTOR.invokeAll(tasks);
        
        // 等待所有任务完成
        int successfulRequests = 0;
        int totalKlins = 0;
        for (Future<List<Kline>> future : futures) {
            List<Kline> klines = future.get();
            if (klines != null && !klines.isEmpty()) {
                successfulRequests++;
                totalKlins += klines.size();
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTimePerRequest = (double) totalTime / NUM_REQUESTS;
        
        System.out.println("总请求数: " + NUM_REQUESTS);
        System.out.println("成功请求数: " + successfulRequests);
        System.out.println("总K线数量: " + totalKlins);
        System.out.println("总耗时: " + totalTime + " ms");
        System.out.println("平均响应时间: " + avgTimePerRequest + " ms");
        System.out.println("吞吐量: " + (NUM_REQUESTS * 1000.0 / totalTime) + " requests/s");
        System.out.println();
    }

    @Test
    void testPatternRecognitionPerformance() throws InterruptedException, ExecutionException {
        System.out.println("=== 测试K线模式识别性能 ===");
        
        List<Callable<PatternResult>> tasks = IntStream.range(0, NUM_REQUESTS)
                .mapToObj(i -> (Callable<PatternResult>) () -> techAnalysisService.recognizePattern(
                        TEST_STOCK_CODE))
                .collect(Collectors.toList());

        long startTime = System.currentTimeMillis();
        List<Future<PatternResult>> futures = EXECUTOR.invokeAll(tasks);
        
        // 等待所有任务完成
        int successfulRequests = 0;
        int threeSwordsFound = 0;
        for (Future<PatternResult> future : futures) {
            PatternResult result = future.get();
            if (result != null) {
                successfulRequests++;
                if (result.isThreeSwordsUnited()) {
                    threeSwordsFound++;
                }
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTimePerRequest = (double) totalTime / NUM_REQUESTS;
        
        System.out.println("总请求数: " + NUM_REQUESTS);
        System.out.println("成功请求数: " + successfulRequests);
        System.out.println("三剑合一信号数: " + threeSwordsFound);
        System.out.println("总耗时: " + totalTime + " ms");
        System.out.println("平均响应时间: " + avgTimePerRequest + " ms");
        System.out.println("吞吐量: " + (NUM_REQUESTS * 1000.0 / totalTime) + " requests/s");
        System.out.println();
    }

    @Test
    void testBatchRealTimeQuotesPerformance() throws InterruptedException, ExecutionException {
        System.out.println("=== 测试批量实时行情获取性能 ===");
        
        List<Callable<List<RealTimeQuote>>> tasks = IntStream.range(0, NUM_REQUESTS)
                .mapToObj(i -> (Callable<List<RealTimeQuote>>) () -> marketDataService.getBatchRealTimeQuotes(TEST_STOCK_CODES))
                .collect(Collectors.toList());

        long startTime = System.currentTimeMillis();
        List<Future<List<RealTimeQuote>>> futures = EXECUTOR.invokeAll(tasks);
        
        // 等待所有任务完成
        int successfulRequests = 0;
        int totalQuotes = 0;
        for (Future<List<RealTimeQuote>> future : futures) {
            List<RealTimeQuote> quotes = future.get();
            if (quotes != null && !quotes.isEmpty()) {
                successfulRequests++;
                totalQuotes += quotes.size();
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTimePerRequest = (double) totalTime / NUM_REQUESTS;
        
        System.out.println("总请求数: " + NUM_REQUESTS);
        System.out.println("成功请求数: " + successfulRequests);
        System.out.println("总行情数量: " + totalQuotes);
        System.out.println("总耗时: " + totalTime + " ms");
        System.out.println("平均响应时间: " + avgTimePerRequest + " ms");
        System.out.println("吞吐量: " + (NUM_REQUESTS * 1000.0 / totalTime) + " requests/s");
        System.out.println();
    }

    @Test
    void testConcurrentMixedOperations() throws InterruptedException, ExecutionException {
        System.out.println("=== 测试并发混合操作性能 ===");
        
        List<Callable<Object>> tasks = new ArrayList<>();
        
        // 添加实时行情获取任务
        tasks.addAll(IntStream.range(0, 25)
                .mapToObj(i -> (Callable<Object>) () -> marketDataService.getRealTimeQuote(TEST_STOCK_CODE))
                .collect(Collectors.toList()));
        
        // 添加历史K线获取任务
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        tasks.addAll(IntStream.range(0, 25)
                .mapToObj(i -> (Callable<Object>) () -> marketDataService.getHistoricalKlines(
                        TEST_STOCK_CODE, startDate, endDate, MarketDataService.KlinePeriod.DAY))
                .collect(Collectors.toList()));
        
        // 添加批量行情获取任务
        tasks.addAll(IntStream.range(0, 25)
                .mapToObj(i -> (Callable<Object>) () -> marketDataService.getBatchRealTimeQuotes(TEST_STOCK_CODES))
                .collect(Collectors.toList()));
        
        // 添加模式识别任务
        tasks.addAll(IntStream.range(0, 25)
                .mapToObj(i -> (Callable<Object>) () -> techAnalysisService.recognizePattern(TEST_STOCK_CODE))
                .collect(Collectors.toList()));
        
        long startTime = System.currentTimeMillis();
        List<Future<Object>> futures = EXECUTOR.invokeAll(tasks);
        
        // 等待所有任务完成
        int successfulRequests = 0;
        for (Future<Object> future : futures) {
            try {
                if (future.get() != null) {
                    successfulRequests++;
                }
            } catch (Exception e) {
                // 忽略异常，只统计成功的请求
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTimePerRequest = (double) totalTime / tasks.size();
        
        System.out.println("总请求数: " + tasks.size());
        System.out.println("成功请求数: " + successfulRequests);
        System.out.println("总耗时: " + totalTime + " ms");
        System.out.println("平均响应时间: " + avgTimePerRequest + " ms");
        System.out.println("吞吐量: " + (tasks.size() * 1000.0 / totalTime) + " requests/s");
    }

    @Test
    void testTechnicalIndicatorsPerformance() throws InterruptedException, ExecutionException {
        System.out.println("=== 测试技术指标计算性能 ===");
        
        // 获取测试数据
        LocalDate startDate = LocalDate.now().minusDays(100);
        LocalDate endDate = LocalDate.now();
        List<Kline> klines = marketDataService.getHistoricalKlines(
                TEST_STOCK_CODE, startDate, endDate, MarketDataService.KlinePeriod.DAY);
        
        if (klines == null || klines.size() < 50) {
            System.out.println("测试数据不足，跳过测试");
            return;
        }
        
        System.out.println("测试K线数量: " + klines.size());
        
        // 测试MA指标计算性能（TA-Lib vs 手动）
        System.out.println("\n--- MA指标计算性能对比 ---");
        int period = 20;
        
        // TA-Lib实现
        long taLibStartTime = System.nanoTime();
        List<Integer> taLibMA = IndicatorCalculator.calculateMA(klines, period, true);
        long taLibEndTime = System.nanoTime();
        long taLibDuration = (taLibEndTime - taLibStartTime) / 1000000;
        
        // 手动实现
        long manualStartTime = System.nanoTime();
        List<Integer> manualMA = IndicatorCalculator.calculateMA(klines, period, false);
        long manualEndTime = System.nanoTime();
        long manualDuration = (manualEndTime - manualStartTime) / 1000000;
        
        System.out.println("TA-Lib MA计算耗时: " + taLibDuration + " ms");
        System.out.println("手动 MA计算耗时: " + manualDuration + " ms");
        System.out.println("TA-Lib 性能提升: " + String.format("%.2f", (double) manualDuration / taLibDuration) + "x");
        
        // 测试RSI指标计算性能（TA-Lib vs 手动）
        System.out.println("\n--- RSI指标计算性能对比 ---");
        period = 14;
        
        // TA-Lib实现
        taLibStartTime = System.nanoTime();
        List<Double> taLibRSI = IndicatorCalculator.calculateRSI(klines, period, true);
        taLibEndTime = System.nanoTime();
        taLibDuration = (taLibEndTime - taLibStartTime) / 1000000;
        
        // 手动实现
        manualStartTime = System.nanoTime();
        List<Double> manualRSI = IndicatorCalculator.calculateRSI(klines, period, false);
        manualEndTime = System.nanoTime();
        manualDuration = (manualEndTime - manualStartTime) / 1000000;
        
        System.out.println("TA-Lib RSI计算耗时: " + taLibDuration + " ms");
        System.out.println("手动 RSI计算耗时: " + manualDuration + " ms");
        System.out.println("TA-Lib 性能提升: " + String.format("%.2f", (double) manualDuration / taLibDuration) + "x");
        
        // 测试MACD指标计算性能（TA-Lib vs 手动）
        System.out.println("\n--- MACD指标计算性能对比 ---");
        
        // TA-Lib实现
        taLibStartTime = System.nanoTime();
        IndicatorCalculator.MACDResult taLibMACD = IndicatorCalculator.calculateMACD(klines, 12, 26, 9, true);
        taLibEndTime = System.nanoTime();
        taLibDuration = (taLibEndTime - taLibStartTime) / 1000000;
        
        // 手动实现
        manualStartTime = System.nanoTime();
        IndicatorCalculator.MACDResult manualMACD = IndicatorCalculator.calculateMACD(klines, 12, 26, 9, false);
        manualEndTime = System.nanoTime();
        manualDuration = (manualEndTime - manualStartTime) / 1000000;
        
        System.out.println("TA-Lib MACD计算耗时: " + taLibDuration + " ms");
        System.out.println("手动 MACD计算耗时: " + manualDuration + " ms");
        System.out.println("TA-Lib 性能提升: " + String.format("%.2f", (double) manualDuration / taLibDuration) + "x");
        
        // 测试BOLL指标计算性能（TA-Lib vs 手动）
        System.out.println("\n--- BOLL指标计算性能对比 ---");
        
        // TA-Lib实现
        taLibStartTime = System.nanoTime();
        IndicatorCalculator.BOLLResult taLibBOLL = IndicatorCalculator.calculateBOLL(klines, 20, 2, true);
        taLibEndTime = System.nanoTime();
        taLibDuration = (taLibEndTime - taLibStartTime) / 1000000;
        
        // 手动实现
        manualStartTime = System.nanoTime();
        IndicatorCalculator.BOLLResult manualBOLL = IndicatorCalculator.calculateBOLL(klines, 20, 2, false);
        manualEndTime = System.nanoTime();
        manualDuration = (manualEndTime - manualStartTime) / 1000000;
        
        System.out.println("TA-Lib BOLL计算耗时: " + taLibDuration + " ms");
        System.out.println("手动 BOLL计算耗时: " + manualDuration + " ms");
        System.out.println("TA-Lib 性能提升: " + String.format("%.2f", (double) manualDuration / taLibDuration) + "x");
        
        // 高并发测试
        System.out.println("\n--- 高并发技术指标计算测试 ---");
        int concurrentRequests = 100;
        List<Callable<Object>> indicatorTasks = IntStream.range(0, concurrentRequests)
                .mapToObj(i -> (Callable<Object>) () -> {
                    IndicatorCalculator.calculateMA(klines, 20, true);
                    IndicatorCalculator.calculateRSI(klines, 14, true);
                    IndicatorCalculator.calculateMACD(klines, 12, 26, 9, true);
                    IndicatorCalculator.calculateBOLL(klines, 20, 2, true);
                    return null;
                })
                .collect(Collectors.toList());
        
        long concurrentStartTime = System.currentTimeMillis();
        List<Future<Object>> concurrentFutures = EXECUTOR.invokeAll(indicatorTasks);
        for (Future<Object> future : concurrentFutures) {
            future.get();
        }
        long concurrentEndTime = System.currentTimeMillis();
        long concurrentTotalTime = concurrentEndTime - concurrentStartTime;
        double avgResponseTime = (double) concurrentTotalTime / concurrentRequests;
        
        System.out.println("并发请求数: " + concurrentRequests);
        System.out.println("总耗时: " + concurrentTotalTime + " ms");
        System.out.println("平均响应时间: " + String.format("%.2f", avgResponseTime) + " ms");
        System.out.println("是否满足实时接口要求(<300ms): " + (avgResponseTime < 300 ? "是" : "否"));
    }

    @Test
    void testDTWAlgorithmPerformance() throws InterruptedException, ExecutionException {
        System.out.println("=== 测试DTW算法性能 ===");
        
        // 生成测试数据
        int sequenceLength = 100;
        double[] sequence1 = IntStream.range(0, sequenceLength)
                .mapToDouble(i -> Math.sin(i * 0.1) + Math.random() * 0.1)
                .toArray();
        double[] sequence2 = IntStream.range(0, sequenceLength + 10)
                .mapToDouble(i -> Math.sin((i - 2) * 0.1) + Math.random() * 0.1)
                .toArray();
        
        System.out.println("序列长度: " + sequenceLength + " / " + (sequenceLength + 10));
        
        // 测试FastDTW算法性能
        long fastDTWStartTime = System.nanoTime();
        double fastDTWDistance = DTWSequenceMatcher.fastDTW(sequence1, sequence2);
        long fastDTWEndTime = System.nanoTime();
        long fastDTWDuration = (fastDTWEndTime - fastDTWStartTime) / 1000000;
        
        System.out.println("FastDTW 距离: " + String.format("%.6f", fastDTWDistance));
        System.out.println("FastDTW 计算耗时: " + fastDTWDuration + " ms");
        System.out.println("是否满足实时接口要求(<300ms): " + (fastDTWDuration < 300 ? "是" : "否"));
        
        // 高并发测试
        System.out.println("\n--- 高并发DTW算法测试 ---");
        int concurrentRequests = 50;
        List<Callable<Double>> dtwTasks = IntStream.range(0, concurrentRequests)
                .mapToObj(i -> (Callable<Double>) () -> DTWSequenceMatcher.fastDTW(sequence1, sequence2))
                .collect(Collectors.toList());
        
        long concurrentStartTime = System.currentTimeMillis();
        List<Future<Double>> concurrentFutures = EXECUTOR.invokeAll(dtwTasks);
        for (Future<Double> future : concurrentFutures) {
            future.get();
        }
        long concurrentEndTime = System.currentTimeMillis();
        long concurrentTotalTime = concurrentEndTime - concurrentStartTime;
        double avgResponseTime = (double) concurrentTotalTime / concurrentRequests;
        
        System.out.println("并发请求数: " + concurrentRequests);
        System.out.println("总耗时: " + concurrentTotalTime + " ms");
        System.out.println("平均响应时间: " + String.format("%.2f", avgResponseTime) + " ms");
        System.out.println("是否满足实时接口要求(<300ms): " + (avgResponseTime < 300 ? "是" : "否"));
    }

    @Test
    void testLargeOrderDetectionPerformance() throws InterruptedException, ExecutionException {
        System.out.println("=== 测试大单异动检测性能 ===");
        
        // 获取测试数据
        RealTimeQuote realTimeQuote = marketDataService.getRealTimeQuote(TEST_STOCK_CODE);
        if (realTimeQuote == null) {
            System.out.println("实时行情数据获取失败，跳过测试");
            return;
        }
        
        // 测试单个大单检测性能
        long singleStartTime = System.nanoTime();
        LargeOrderResult singleResult = largeOrderDetector.detectLargeOrders(realTimeQuote);
        long singleEndTime = System.nanoTime();
        long singleDuration = (singleEndTime - singleStartTime) / 1000000;
        
        System.out.println("单个大单检测耗时: " + singleDuration + " ms");
        System.out.println("是否满足实时接口要求(<300ms): " + (singleDuration < 300 ? "是" : "否"));
        
        // 高并发测试
        System.out.println("\n--- 高并发大单检测测试 ---");
        int concurrentRequests = 100;
        List<Callable<LargeOrderResult>> largeOrderTasks = IntStream.range(0, concurrentRequests)
                .mapToObj(i -> (Callable<LargeOrderResult>) () -> largeOrderDetector.detectLargeOrders(realTimeQuote))
                .collect(Collectors.toList());
        
        long concurrentStartTime = System.currentTimeMillis();
        List<Future<LargeOrderResult>> concurrentFutures = EXECUTOR.invokeAll(largeOrderTasks);
        for (Future<LargeOrderResult> future : concurrentFutures) {
            future.get();
        }
        long concurrentEndTime = System.currentTimeMillis();
        long concurrentTotalTime = concurrentEndTime - concurrentStartTime;
        double avgResponseTime = (double) concurrentTotalTime / concurrentRequests;
        
        System.out.println("并发请求数: " + concurrentRequests);
        System.out.println("总耗时: " + concurrentTotalTime + " ms");
        System.out.println("平均响应时间: " + String.format("%.2f", avgResponseTime) + " ms");
        System.out.println("是否满足实时接口要求(<300ms): " + (avgResponseTime < 300 ? "是" : "否"));
    }

    @Test
    void testCompleteSystemPerformance() throws InterruptedException, ExecutionException {
        System.out.println("=== 测试完整系统混合操作性能 ===");
        
        // 获取测试数据
        LocalDate startDate = LocalDate.now().minusDays(100);
        LocalDate endDate = LocalDate.now();
        List<Kline> klines = marketDataService.getHistoricalKlines(
                TEST_STOCK_CODE, startDate, endDate, MarketDataService.KlinePeriod.DAY);
        RealTimeQuote realTimeQuote = marketDataService.getRealTimeQuote(TEST_STOCK_CODE);
        
        if (klines == null || klines.size() < 50 || realTimeQuote == null) {
            System.out.println("测试数据不足，跳过测试");
            return;
        }
        
        // 生成DTW测试数据
        int sequenceLength = 50;
        double[] sequence1 = IntStream.range(0, sequenceLength)
                .mapToDouble(i -> Math.sin(i * 0.2) + Math.random() * 0.1)
                .toArray();
        double[] sequence2 = IntStream.range(0, sequenceLength + 5)
                .mapToDouble(i -> Math.sin((i - 1) * 0.2) + Math.random() * 0.1)
                .toArray();
        
        // 创建混合任务
        List<Callable<Object>> mixedTasks = new ArrayList<>();
        
        // 添加筹码分析任务
        mixedTasks.addAll(IntStream.range(0, 15)
                .mapToObj(i -> (Callable<Object>) () -> chipAnalysisService.calculateDistribution(TEST_STOCK_CODE))
                .collect(Collectors.toList()));
        
        // 添加技术指标计算任务
        mixedTasks.addAll(IntStream.range(0, 15)
                .mapToObj(i -> (Callable<Object>) () -> {
                    IndicatorCalculator.calculateMA(klines, 20, true);
                    IndicatorCalculator.calculateRSI(klines, 14, true);
                    return null;
                })
                .collect(Collectors.toList()));
        
        // 添加DTW算法任务
        mixedTasks.addAll(IntStream.range(0, 10)
                .mapToObj(i -> (Callable<Object>) () -> DTWSequenceMatcher.fastDTW(sequence1, sequence2))
                .collect(Collectors.toList()));
        
        // 添加大单异动检测任务
        mixedTasks.addAll(IntStream.range(0, 10)
                .mapToObj(i -> (Callable<Object>) () -> largeOrderDetector.detectLargeOrders(realTimeQuote))
                .collect(Collectors.toList()));
        
        // 添加模式识别任务
        mixedTasks.addAll(IntStream.range(0, 10)
                .mapToObj(i -> (Callable<Object>) () -> techAnalysisService.recognizePattern(TEST_STOCK_CODE))
                .collect(Collectors.toList()));
        
        // 添加实时行情获取任务
        mixedTasks.addAll(IntStream.range(0, 20)
                .mapToObj(i -> (Callable<Object>) () -> marketDataService.getRealTimeQuote(TEST_STOCK_CODE))
                .collect(Collectors.toList()));
        
        // 添加历史K线获取任务
        mixedTasks.addAll(IntStream.range(0, 20)
                .mapToObj(i -> (Callable<Object>) () -> marketDataService.getHistoricalKlines(
                        TEST_STOCK_CODE, startDate, endDate, MarketDataService.KlinePeriod.DAY))
                .collect(Collectors.toList()));
        
        System.out.println("总任务数: " + mixedTasks.size());
        System.out.println("筹码分析任务: 15个");
        System.out.println("技术指标计算任务: 15个");
        System.out.println("DTW算法任务: 10个");
        System.out.println("大单异动检测任务: 10个");
        System.out.println("模式识别任务: 10个");
        System.out.println("实时行情获取任务: 20个");
        System.out.println("历史K线获取任务: 20个");
        
        long startTime = System.currentTimeMillis();
        List<Future<Object>> mixedFutures = EXECUTOR.invokeAll(mixedTasks);
        
        int successfulTasks = 0;
        for (Future<Object> future : mixedFutures) {
            try {
                if (future.get() != null) {
                    successfulTasks++;
                }
            } catch (Exception e) {
                // 忽略异常，只统计成功的任务
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgResponseTime = (double) totalTime / mixedTasks.size();
        
        System.out.println("\n执行结果:");
        System.out.println("成功任务数: " + successfulTasks + "/" + mixedTasks.size());
        System.out.println("总耗时: " + totalTime + " ms");
        System.out.println("平均响应时间: " + String.format("%.2f", avgResponseTime) + " ms");
        System.out.println("是否满足系统整体性能要求: " + (avgResponseTime < 1000 ? "是" : "否"));
        System.out.println("(实时接口要求<300ms, 非实时接口要求<1000ms)");
    }
}
