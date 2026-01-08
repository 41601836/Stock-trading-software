import com.stock.analysis.module.chip.ChipAnalysisServiceImpl;
import com.stock.analysis.module.chip.StockChip;
import com.stock.analysis.module.tech.DTWUtil;
import com.stock.analysis.module.tech.IndicatorCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimplePerformanceTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 量化交易系统性能测试 ===");
        
        // 测试WAD筹码分布模型
        testWADModelPerformance();
        
        // 测试DTW算法性能
        testDTWAlgorithmPerformance();
        
        // 测试技术指标计算性能
        testIndicatorCalculationPerformance();
        
        // 测试大单异动检测性能
        testLargeOrderDetectionPerformance();
        
        System.out.println("\n=== 性能测试完成 ===");
    }

    /**
     * 测试WAD筹码分布模型性能
     */
    private static void testWADModelPerformance() throws Exception {
        System.out.println("\n1. WAD筹码分布模型性能测试");
        
        ChipAnalysisServiceImpl chipService = new ChipAnalysisServiceImpl();
        
        // 单线程测试
        long startTime = System.currentTimeMillis();
        StockChip result = chipService.calculateDistribution("600000");
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.printf("   单线程执行时间: %d ms (要求 < 1000 ms)\n", duration);
        System.out.printf("   测试结果: 主筹成本=%.2f元, 集中度=%.2f\n", result.getMainCost() / 100.0, result.getConcentrationRatio());
        
        // 并发测试
        int threadCount = 10;
        int requestsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        chipService.calculateDistribution("600000");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        
        int totalRequests = threadCount * requestsPerThread;
        double throughput = (double) totalRequests / (duration / 1000.0);
        
        System.out.printf("   并发测试: %d线程 × %d请求 = %d请求\n", threadCount, requestsPerThread, totalRequests);
        System.out.printf("   总执行时间: %d ms\n", duration);
        System.out.printf("   吞吐量: %.2f请求/秒\n", throughput);
        
        executor.shutdown();
    }

    /**
     * 测试DTW算法性能
     */
    private static void testDTWAlgorithmPerformance() {
        System.out.println("\n2. DTW算法性能测试");
        
        // 创建两个随机序列
        List<Double> s1 = generateRandomSequence(100);
        List<Double> s2 = generateRandomSequence(100);
        
        // 测试DTW算法
        long startTime = System.currentTimeMillis();
        double distance = DTWUtil.calculateDTW(s1, s2, 0.2);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.printf("   DTW距离: %.4f\n", distance);
        System.out.printf("   执行时间: %d ms (要求 < 300 ms)\n", duration);
        
        // 测试缓存效果
        startTime = System.currentTimeMillis();
        double distanceCached = DTWUtil.calculateDTW(s1, s2, 0.2);
        endTime = System.currentTimeMillis();
        long cachedDuration = endTime - startTime;
        
        System.out.printf("   缓存后执行时间: %d ms\n", cachedDuration);
        System.out.printf("   缓存性能提升: %.2f倍\n", (double) duration / cachedDuration);
    }

    /**
     * 测试技术指标计算性能
     */
    private static void testIndicatorCalculationPerformance() {
        System.out.println("\n3. 技术指标计算性能测试");
        
        // 创建随机K线数据
        List<IndicatorCalculator.Kline> klines = generateRandomKlins(1000);
        
        long startTime = System.currentTimeMillis();
        
        // 计算多个指标
        IndicatorCalculator.calculateMA(klines, 5);
        IndicatorCalculator.calculateMA(klines, 10);
        IndicatorCalculator.calculateMA(klines, 20);
        IndicatorCalculator.calculateMA(klines, 60);
        IndicatorCalculator.calculateMA(klines, 120);
        
        IndicatorCalculator.calculateRSI(klines, 14);
        IndicatorCalculator.calculateMACD(klines);
        IndicatorCalculator.calculateBOLL(klines, 20, 2);
        IndicatorCalculator.calculateATR(klines, 14);
        IndicatorCalculator.calculateWR(klines, 14);
        IndicatorCalculator.calculateKDJ(klines, 9, 3, 3);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.printf("   计算1000条K线的12个指标执行时间: %d ms (要求 < 300 ms)\n", duration);
        System.out.printf("   单条K线平均处理时间: %.2f微秒\n", (double) duration * 1000 / klines.size());
    }

    /**
     * 生成随机序列
     */
    private static List<Double> generateRandomSequence(int length) {
        List<Double> sequence = new ArrayList<>();
        double base = 10.0;
        Random random = new Random();
        
        for (int i = 0; i < length; i++) {
            double value = base + random.nextDouble() * 2.0 - 1.0; // -1.0 to 1.0
            sequence.add(value);
            base = value;
        }
        
        return sequence;
    }

    /**
     * 生成随机K线数据
     */
    private static List<IndicatorCalculator.Kline> generateRandomKlins(int count) {
        List<IndicatorCalculator.Kline> klines = new ArrayList<>();
        double basePrice = 10.0;
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            double open = basePrice + random.nextDouble() - 0.5; // -0.5 to 0.5
            double high = open + random.nextDouble(); // 0 to 1.0
            double low = open - random.nextDouble(); // 0 to 1.0
            double close = low + random.nextDouble() * (high - low);
            double volume = 100000 + random.nextDouble() * 9900000; // 100000 to 10000000
            
            IndicatorCalculator.Kline kline = new IndicatorCalculator.Kline();
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
}