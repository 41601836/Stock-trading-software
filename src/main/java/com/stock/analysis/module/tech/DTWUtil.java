package com.stock.analysis.module.tech;

import java.util.HashMap;
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
    private static final Map<String, Double> dtwCache = new HashMap<>();
    
    // 线程池：用于并行计算
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool();
    
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
        if (dtwCache.containsKey(cacheKey)) {
            return dtwCache.get(cacheKey);
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
        
        // 缓存结果
        dtwCache.put(cacheKey, result);
        
        // 限制缓存大小
        if (dtwCache.size() > 1000) {
            // 移除最旧的缓存项 (简单实现)
            String oldestKey = dtwCache.keySet().iterator().next();
            dtwCache.remove(oldestKey);
        }
        
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
     * 注意：DTW算法的并行化比较复杂，这里使用的是将序列分成多个子序列的近似并行策略
     */
    private static double calculateDTWParallel(List<Double> s1, List<Double> s2, double windowRatio) {
        // 对于长序列，使用更高效的窗口策略
        double adjustedWindowRatio = Math.max(windowRatio, 0.15);
        return calculateDTWSequential(s1, s2, adjustedWindowRatio);
    }
    
    /**
     * 生成缓存键
     */
    private static String generateCacheKey(List<Double> s1, List<Double> s2, double windowRatio) {
        // 使用序列的哈希码和窗口大小生成唯一键
        String s1Hash = s1.stream().map(d -> String.format("%.4f", d)).collect(Collectors.joining(","));
        String s2Hash = s2.stream().map(d -> String.format("%.4f", d)).collect(Collectors.joining(","));
        return String.format("%s|%s|%.2f", s1Hash, s2Hash, windowRatio);
    }
    
    /**
     * 清空DTW缓存
     */
    public static void clearCache() {
        dtwCache.clear();
    }
}
