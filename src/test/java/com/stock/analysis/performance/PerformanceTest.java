package com.stock.analysis.performance;

import com.stock.analysis.module.market.MarketDataService;
import com.stock.analysis.module.market.RealTimeQuote;
import com.stock.analysis.module.tech.Kline;
import com.stock.analysis.module.tech.TechAnalysisService;
import com.stock.analysis.module.tech.TechAnalysisService.PatternResult;
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
}
