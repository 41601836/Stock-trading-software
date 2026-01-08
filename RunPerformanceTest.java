import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 性能测试主类 - 验证各算法模块是否满足实时要求
 */
public class RunPerformanceTest {

    public static void main(String[] args) {
        System.out.println("=== 开始性能测试 ===");
        System.out.println("测试日期: " + new java.util.Date());
        System.out.println();
        
        // 测试DTW算法性能
        testDTWPerformance();
        
        // 测试技术指标计算性能
        testIndicatorPerformance();
        
        // 测试时间衰减计算性能
        testTimeDecayPerformance();
        
        System.out.println();
        System.out.println("=== 性能测试结束 ===");
    }
    
    /**
     * 测试DTW序列匹配性能
     */
    private static void testDTWPerformance() {
        System.out.println("=== 测试DTW序列匹配性能 ===");
        
        int sequenceLength = 100;
        int testRuns = 10;
        
        // 创建两个测试序列
        List<Double> sequence1 = generateRandomSequence(sequenceLength);
        List<Double> sequence2 = generateRandomSequence(sequenceLength);
        
        long totalTime = 0;
        
        for (int i = 0; i < testRuns; i++) {
            long startTime = System.currentTimeMillis();
            
            // 执行DTW计算
            double distance = com.stock.analysis.module.tech.DTWUtil.calculateDTW(sequence1, sequence2);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            totalTime += duration;
            System.out.println("测试 " + (i+1) + ": DTW距离 = " + distance + ", 耗时 = " + duration + " ms");
        }
        
        double averageTime = (double) totalTime / testRuns;
        System.out.println("DTW序列匹配平均时间: " + averageTime + " ms");
        System.out.println("是否满足实时要求(300ms): " + (averageTime < 300 ? "是" : "否"));
    }
    
    /**
     * 测试技术指标计算性能
     */
    private static void testIndicatorPerformance() {
        System.out.println("\n=== 测试技术指标计算性能 ===");
        
        int klineCount = 500;
        int testRuns = 5;
        
        // 创建测试K线数据
        List<com.stock.analysis.module.tech.Kline> klines = generateTestKlines(klineCount);
        
        long totalTime = 0;
        
        for (int i = 0; i < testRuns; i++) {
            long startTime = System.currentTimeMillis();
            
            // 计算MA
            com.stock.analysis.module.tech.IndicatorCalculator.calculateMA(klines, 20);
            
            // 计算RSI
            com.stock.analysis.module.tech.IndicatorCalculator.calculateRSI(klines, 14);
            
            // 计算MACD
            com.stock.analysis.module.tech.IndicatorCalculator.calculateMACD(klines, 12, 26, 9);
            
            // 计算BOLL
            com.stock.analysis.module.tech.IndicatorCalculator.calculateBOLL(klines, 20, 2);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            totalTime += duration;
            System.out.println("测试 " + (i+1) + ": 计算完成, 耗时 = " + duration + " ms");
        }
        
        double averageTime = (double) totalTime / testRuns;
        System.out.println("技术指标计算平均时间: " + averageTime + " ms");
        System.out.println("是否满足实时要求(300ms): " + (averageTime < 300 ? "是" : "否"));
    }
    
    /**
     * 测试时间衰减计算性能
     */
    private static void testTimeDecayPerformance() {
        System.out.println("\n=== 测试时间衰减计算性能 ===");
        
        int testRuns = 100000;
        
        long startTime = System.currentTimeMillis();
        
        // 模拟大量时间衰减计算
        for (int i = 0; i < testRuns; i++) {
            double timeDiffHours = Math.random() * 168; // 0-7天
            double baseLambda = Math.random() * 0.1 + 0.05;
            double marketActivityAdjustment = Math.random() * 0.5 + 0.75;
            
            // 使用优化后的时间衰减算法
            double decay = com.stock.analysis.module.chip.ChipAnalysisServiceImpl.calculateFastTimeDecay(
                timeDiffHours, baseLambda, marketActivityAdjustment);
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double averageTimePerCall = (double) totalTime / testRuns * 1000; // 微秒
        
        System.out.println("完成 " + testRuns + " 次时间衰减计算");
        System.out.println("总耗时: " + totalTime + " ms");
        System.out.println("单次调用平均时间: " + averageTimePerCall + " 微秒");
        System.out.println("是否满足实时要求: " + (averageTimePerCall < 1 ? "是" : "否"));
    }
    
    /**
     * 生成随机序列
     */
    private static List<Double> generateRandomSequence(int length) {
        Random random = new Random();
        List<Double> sequence = new ArrayList<>(length);
        
        double price = 100.0;
        for (int i = 0; i < length; i++) {
            // 模拟价格波动 (-2% 到 +2%)
            double change = (random.nextDouble() - 0.5) * 0.04;
            price *= (1 + change);
            sequence.add(price);
        }
        
        return sequence;
    }
    
    /**
     * 生成测试K线数据
     */
    private static List<com.stock.analysis.module.tech.Kline> generateTestKlines(int count) {
        Random random = new Random();
        List<com.stock.analysis.module.tech.Kline> klines = new ArrayList<>(count);
        
        double currentPrice = 100.0;
        java.time.LocalDate date = java.time.LocalDate.now();
        
        for (int i = 0; i < count; i++) {
            // 生成OHLC数据
            double open = currentPrice;
            double high = open * (1 + random.nextDouble() * 0.03);
            double low = open * (1 - random.nextDouble() * 0.03);
            double close = low + random.nextDouble() * (high - low);
            long volume = (long) (random.nextDouble() * 1000000 + 10000);
            
            // 更新当前价格
            currentPrice = close;
            
            // 创建Kline对象
            com.stock.analysis.module.tech.Kline kline = new com.stock.analysis.module.tech.Kline();
            kline.setDate(date);
            kline.setOpen((int) (open * 100));
            kline.setHigh((int) (high * 100));
            kline.setLow((int) (low * 100));
            kline.setClose((int) (close * 100));
            kline.setVolume(volume);
            kline.setAmount((long) (close * volume * 100)); // 分
            
            klines.add(kline);
            
            // 增加一天
            date = date.plusDays(1);
        }
        
        return klines;
    }
}