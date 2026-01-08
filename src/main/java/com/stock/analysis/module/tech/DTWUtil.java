package com.stock.analysis.module.tech;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * 动态时间规整 (DTW) 算法工具类 - 高性能版
 * 包含并行计算、早期终止和缓存优化
 */
public class DTWUtil {

    // 默认窗口大小 (序列长度的10%)
    private static final double DEFAULT_WINDOW_RATIO = 0.1;
    
    // 并行计算阈值：当序列长度超过此值时启用并行计算
    private static final int PARALLEL_THRESHOLD = 100;
    
    // 早期终止阈值：当当前路径成本超过此值时终止计算
    private static final double EARLY_TERMINATION_THRESHOLD = 50.0;
    
    // 缓存：存储常用序列对的DTW距离
    private static final Map<String, CacheEntry> dtwCache = new LinkedHashMap<String, CacheEntry>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            // 当缓存大小超过2000时，移除最旧的条目
            return size() > 2000;
        }
    };
    
    // 缓存条目类，包含缓存值和过期时间
    private static class CacheEntry {
        private final double value;
        private final long expirationTime; // 过期时间（毫秒）
        
        public CacheEntry(double value, long ttlMs) {
            this.value = value;
            this.expirationTime = System.currentTimeMillis() + ttlMs;
        }
        
        public double getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
    
    // 线程池：用于并行计算
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 2);
    
    // 特征空间降维阈值：当序列长度超过此值时进行降维
    private static final int DIMENSIONALITY_REDUCTION_THRESHOLD = 300;
    
    // 降维因子
    private static final double REDUCTION_FACTOR = 0.5;
    
    /**
     * 计算两个序列的 DTW 距离 (高性能版)
     * @param s1 序列1 (如当前K线收盘价序列)
     * @param s2 序列2 (如标准暴涨模型序列)
     * @return 距离 (越小越相似)
     */
    public static double calculateDTW(List<Double> s1, List<Double> s2) {
        return calculateDTW(s1, s2, DEFAULT_WINDOW_RATIO);
    }
    
    /**
     * 计算两个序列的 DTW 距离 (高性能版，支持自定义窗口大小)
     * @param s1 序列1 (如当前K线收盘价序列)
     * @param s2 序列2 (如标准暴涨模型序列)
     * @param windowRatio 窗口大小比例 (0.0-1.0，推荐0.1-0.2)
     * @return 距离 (越小越相似)
     */
    public static double calculateDTW(List<Double> s1, List<Double> s2, double windowRatio) {
        int n = s1.size();
        int m = s2.size();
        
        if (n == 0 || m == 0) return Double.MAX_VALUE;
        
        // 缓存键：使用序列的哈希码组合
        String cacheKey = generateCacheKey(s1, s2, windowRatio);
        
        // 检查缓存
        CacheEntry entry = dtwCache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        
        // 根据序列长度选择计算策略
        double result;
        if (n > PARALLEL_THRESHOLD || m > PARALLEL_THRESHOLD) {
            // 长序列使用并行计算
            result = calculateDTWParallel(s1, s2, windowRatio);
        } else {
            // 短序列使用普通计算
            result = calculateDTWSequential(s1, s2, windowRatio);
        }
        
        // 缓存结果（5分钟过期）
        dtwCache.put(cacheKey, new CacheEntry(result, 5 * 60 * 1000));
        
        // 清理过期缓存条目
        cleanupExpiredCache();
        
        return result;
    }
    
    /**
     * 顺序计算 DTW 距离 (带早期终止优化)
     */
    private static double calculateDTWSequential(List<Double> s1, List<Double> s2, double windowRatio) {
        int n = s1.size();
        int m = s2.size();
        
        // 计算窗口大小
        int w = (int) Math.max(windowRatio * Math.max(n, m), 1);
        
        // 优化空间使用：只存储当前行和前一行
        double[] prev = new double[m + 1];
        double[] curr = new double[m + 1];
        
        // 初始化
        for (int j = 0; j <= m; j++) {
            prev[j] = Double.MAX_VALUE;
            curr[j] = Double.MAX_VALUE;
        }
        prev[0] = 0;
        
        // 动态规划计算 (带早期终止)
        for (int i = 1; i <= n; i++) {
            // 计算当前行的有效列范围
            int jStart = Math.max(1, i - w);
            int jEnd = Math.min(m, i + w);
            
            // 重置当前行
            for (int j = 0; j < jStart; j++) {
                curr[j] = Double.MAX_VALUE;
            }
            
            boolean hasValidPath = false;
            double currentMin = Double.MAX_VALUE;
            
            for (int j = jStart; j <= jEnd; j++) {
                double cost = Math.abs(s1.get(i - 1) - s2.get(j - 1));
                
                // 计算当前点的最小成本
                double minCost = prev[j]; // 插入
                if (j > 0) {
                    minCost = Math.min(minCost, curr[j - 1]); // 删除
                    minCost = Math.min(minCost, prev[j - 1]); // 匹配
                }
                
                curr[j] = cost + (minCost == Double.MAX_VALUE ? 0 : minCost);
                
                // 跟踪当前行的最小成本
                if (curr[j] < currentMin) {
                    currentMin = curr[j];
                }
                
                // 检查是否有有效路径
                if (curr[j] != Double.MAX_VALUE) {
                    hasValidPath = true;
                }
            }
            
            // 超过窗口范围的列设为最大值
            for (int j = jEnd + 1; j <= m; j++) {
                curr[j] = Double.MAX_VALUE;
            }
            
            // 早期终止：如果当前路径成本超过阈值，终止计算
            if (currentMin > EARLY_TERMINATION_THRESHOLD) {
                return Double.MAX_VALUE;
            }
            
            // 如果没有有效路径，终止计算
            if (!hasValidPath) {
                return Double.MAX_VALUE;
            }
            
            // 交换当前行和前一行
            double[] temp = prev;
            prev = curr;
            curr = temp;
        }
        
        return prev[m];
    }
    
    /**
     * 并行计算 DTW 距离 (使用ForkJoin框架)
     * 实现真正的并行DTW算法，将序列分割成多个子问题
     */
    private static double calculateDTWParallel(List<Double> s1, List<Double> s2, double windowRatio) {
        // 对长序列进行降维处理
        List<Double> reducedS1 = reduceDimensionality(s1);
        List<Double> reducedS2 = reduceDimensionality(s2);
        
        // 对于长序列，使用更高效的窗口策略
        double adjustedWindowRatio = Math.max(windowRatio, 0.15);
        
        // 使用ForkJoin框架进行并行计算
        DTWTask task = new DTWTask(reducedS1, reducedS2, adjustedWindowRatio);
        return forkJoinPool.invoke(task);
    }
    
    /**
     * 序列降维处理
     * 对过长的序列进行降维，减少计算量
     */
    private static List<Double> reduceDimensionality(List<Double> sequence) {
        int n = sequence.size();
        if (n <= DIMENSIONALITY_REDUCTION_THRESHOLD) {
            return sequence; // 不需要降维
        }
        
        // 计算降维后的序列长度
        int reducedLength = (int) (n * REDUCTION_FACTOR);
        if (reducedLength < 10) {
            reducedLength = 10; // 确保降维后的序列至少有10个点
        }
        
        // 使用滑动窗口平均法进行降维，保持序列的趋势特征
        List<Double> reducedSequence = new ArrayList<>();
        int windowSize = n / reducedLength;
        
        for (int i = 0; i < reducedLength; i++) {
            int startIdx = i * windowSize;
            int endIdx = Math.min((i + 1) * windowSize, n);
            
            // 计算窗口内的平均值
            double sum = 0;
            for (int j = startIdx; j < endIdx; j++) {
                sum += sequence.get(j);
            }
            reducedSequence.add(sum / (endIdx - startIdx));
        }
        
        // 处理剩余的点
        if (reducedSequence.size() < reducedLength) {
            int startIdx = reducedLength * windowSize;
            if (startIdx < n) {
                double sum = 0;
                for (int j = startIdx; j < n; j++) {
                    sum += sequence.get(j);
                }
                reducedSequence.add(sum / (n - startIdx));
            }
        }
        
        return reducedSequence;
    }
    
    /**
     * DTW并行计算任务
     * 基于ForkJoin框架的递归并行任务
     */
    private static class DTWTask extends RecursiveTask<Double> {
        private static final long serialVersionUID = 1L;
        
        private static final int TASK_THRESHOLD = 50; // 子任务分割阈值
        
        private final List<Double> s1;
        private final List<Double> s2;
        private final double windowRatio;
        
        public DTWTask(List<Double> s1, List<Double> s2, double windowRatio) {
            this.s1 = s1;
            this.s2 = s2;
            this.windowRatio = windowRatio;
        }
        
        @Override
        protected Double compute() {
            int n = s1.size();
            int m = s2.size();
            
            // 如果序列长度小于阈值，直接使用顺序计算
            if (n <= TASK_THRESHOLD && m <= TASK_THRESHOLD) {
                return calculateDTWSequential(s1, s2, windowRatio);
            }
            
            // 将序列分割为子序列进行并行计算
            if (n > m) {
                // 如果s1更长，分割s1
                int mid = n / 2;
                List<Double> s1Left = s1.subList(0, mid);
                List<Double> s1Right = s1.subList(mid, n);
                
                DTWTask leftTask = new DTWTask(s1Left, s2, windowRatio);
                DTWTask rightTask = new DTWTask(s1Right, s2, windowRatio);
                
                // 并行执行两个子任务
                leftTask.fork();
                double rightResult = rightTask.compute();
                double leftResult = leftTask.join();
                
                // 合并结果：取两个子结果的加权平均值
                return (leftResult * mid + rightResult * (n - mid)) / n;
            } else {
                // 如果s2更长，分割s2
                int mid = m / 2;
                List<Double> s2Left = s2.subList(0, mid);
                List<Double> s2Right = s2.subList(mid, m);
                
                DTWTask leftTask = new DTWTask(s1, s2Left, windowRatio);
                DTWTask rightTask = new DTWTask(s1, s2Right, windowRatio);
                
                // 并行执行两个子任务
                leftTask.fork();
                double rightResult = rightTask.compute();
                double leftResult = leftTask.join();
                
                // 合并结果：取两个子结果的加权平均值
                return (leftResult * mid + rightResult * (m - mid)) / m;
            }
        }
    }
    
    /**
     * 生成缓存键
     */
    private static String generateCacheKey(List<Double> s1, List<Double> s2, double windowRatio) {
        // 使用高效的哈希算法生成缓存键，避免生成过长的字符串
        int s1HashCode = calculateSequenceHashCode(s1);
        int s2HashCode = calculateSequenceHashCode(s2);
        return String.format("%d|%d|%.2f", s1HashCode, s2HashCode, windowRatio);
    }
    
    /**
     * 计算序列的哈希码
     */
    private static int calculateSequenceHashCode(List<Double> sequence) {
        int result = 1;
        int step = Math.max(1, sequence.size() / 10); // 采样计算，提高效率
        for (int i = 0; i < sequence.size(); i += step) {
            long bits = Double.doubleToLongBits(sequence.get(i));
            result = 31 * result + (int) (bits ^ (bits >>> 32));
        }
        return result;
    }
    
    /**
     * 清理过期的缓存条目
     */
    private static void cleanupExpiredCache() {
        // 每100次调用清理一次过期缓存
        if (dtwCache.size() % 100 == 0) {
            List<String> expiredKeys = new ArrayList<>();
            for (Map.Entry<String, CacheEntry> entry : dtwCache.entrySet()) {
                if (entry.getValue().isExpired()) {
                    expiredKeys.add(entry.getKey());
                }
            }
            
            for (String key : expiredKeys) {
                dtwCache.remove(key);
            }
        }
    }
    
    /**
     * 清空DTW缓存
     */
    public static void clearCache() {
        dtwCache.clear();
    }
}
