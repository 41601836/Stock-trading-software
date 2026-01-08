package com.stock.analysis.module.tech;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTW (动态时间规整) 序列匹配器
 * 用于K线序列或技术指标序列的相似性匹配
 */
@Slf4j
@Component
public class DTWSequenceMatcher {

    // DTW匹配结果类
    public static class DTWMatchResult {
        private final double distance; // DTW距离，越小越相似
        private final double similarity; // 相似度 (0-1)
        private final List<int[]> alignmentPath; // 对齐路径
        private final long computationTimeMs; // 计算时间(毫秒)

        public DTWMatchResult(double distance, double similarity, List<int[]> alignmentPath, long computationTimeMs) {
            this.distance = distance;
            this.similarity = similarity;
            this.alignmentPath = alignmentPath;
            this.computationTimeMs = computationTimeMs;
        }

        public double getDistance() { return distance; }
        public double getSimilarity() { return similarity; }
        public List<int[]> getAlignmentPath() { return alignmentPath; }
        public long getComputationTimeMs() { return computationTimeMs; }
    }

    /**
     * 计算两个序列的DTW距离（默认使用FastDTW）
     * @param sequence1 第一个序列
     * @param sequence2 第二个序列
     * @return DTW匹配结果
     */
    public DTWMatchResult computeDTW(List<Double> sequence1, List<Double> sequence2) {
        // 默认使用FastDTW算法，性能更优
        return computeFastDTW(sequence1, sequence2, Double.MAX_VALUE, 1.0);
    }

    /**
     * 计算两个序列的DTW距离（带约束）
     * @param sequence1 第一个序列
     * @param sequence2 第二个序列
     * @param maxDistance 最大允许距离（超过则提前终止）
     * @param windowRatio 搜索窗口比例（用于性能优化）
     * @return DTW匹配结果
     */
    public DTWMatchResult computeDTW(List<Double> sequence1, List<Double> sequence2, 
                                     double maxDistance, double windowRatio) {
        // 对于短序列直接使用传统DTW，长序列使用FastDTW
        int length1 = sequence1 != null ? sequence1.size() : 0;
        int length2 = sequence2 != null ? sequence2.size() : 0;
        
        if (length1 > 100 || length2 > 100) {
            return computeFastDTW(sequence1, sequence2, maxDistance, windowRatio);
        } else {
            return computeClassicDTW(sequence1, sequence2, maxDistance, windowRatio);
        }
    }

    /**
     * 计算两个序列的DTW距离（传统DTW算法）
     * @param sequence1 第一个序列
     * @param sequence2 第二个序列
     * @param maxDistance 最大允许距离（超过则提前终止）
     * @param windowRatio 搜索窗口比例（用于性能优化）
     * @return DTW匹配结果
     */
    public DTWMatchResult computeClassicDTW(List<Double> sequence1, List<Double> sequence2, 
                                     double maxDistance, double windowRatio) {
        if (sequence1 == null || sequence2 == null || sequence1.isEmpty() || sequence2.isEmpty()) {
            return new DTWMatchResult(Double.MAX_VALUE, 0.0, new ArrayList<>(), 0);
        }

        long startTime = System.currentTimeMillis();

        // 将List转换为数组以提高性能
        double[] seq1 = sequence1.stream().mapToDouble(d -> d).toArray();
        double[] seq2 = sequence2.stream().mapToDouble(d -> d).toArray();

        int n = seq1.length;
        int m = seq2.length;

        // 初始化DTW矩阵 - 只保存两行以节省内存
        double[][] dtw = new double[2][m];
        Arrays.fill(dtw[0], Double.MAX_VALUE);
        Arrays.fill(dtw[1], Double.MAX_VALUE);

        // 设置起始点
        dtw[0][0] = Math.pow(seq1[0] - seq2[0], 2);

        // 计算搜索窗口大小
        int windowSize = (int) (Math.max(n, m) * windowRatio);

        // 填充DTW矩阵
        for (int i = 1; i < n; i++) {
            // 计算当前行和前一行的索引
            int currentRow = i % 2;
            int prevRow = (i - 1) % 2;
            
            // 重置当前行
            Arrays.fill(dtw[currentRow], Double.MAX_VALUE);
            
            for (int j = Math.max(1, i - windowSize); j < Math.min(m, i + windowSize); j++) {
                double cost = Math.pow(seq1[i] - seq2[j], 2);
                dtw[currentRow][j] = cost + Math.min(Math.min(dtw[prevRow][j], dtw[currentRow][j-1]), dtw[prevRow][j-1]);

                // 提前终止条件
                if (dtw[currentRow][j] > maxDistance) {
                    long endTime = System.currentTimeMillis();
                    return new DTWMatchResult(Double.MAX_VALUE, 0.0, new ArrayList<>(), endTime - startTime);
                }
            }
        }

        // 计算相似度（将距离转换为0-1范围）
        double distance = dtw[(n - 1) % 2][m - 1];
        double maxPossibleDistance = Math.max(getMaxSequenceValue(seq1), getMaxSequenceValue(seq2)) * Math.max(n, m);
        double similarity = maxPossibleDistance > 0 ? 1.0 - Math.min(1.0, distance / maxPossibleDistance) : 1.0;

        // 回溯获取对齐路径（需要重新计算完整矩阵，仅在需要路径时使用）
        List<int[]> alignmentPath = new ArrayList<>();
        if (similarity > 0.1) { // 只有相似度较高时才计算路径
            alignmentPath = getAlignmentPathWithWindow(seq1, seq2, windowSize, maxDistance);
        }

        long endTime = System.currentTimeMillis();

        log.debug("DTW计算完成: 距离={}, 相似度={}, 计算时间={}ms", distance, similarity, endTime - startTime);

        return new DTWMatchResult(distance, similarity, alignmentPath, endTime - startTime);
    }

    /**
     * 计算两个序列的DTW距离（FastDTW算法）
     * FastDTW使用多级近似策略来加速计算
     * @param sequence1 第一个序列
     * @param sequence2 第二个序列
     * @param maxDistance 最大允许距离（超过则提前终止）
     * @param windowRatio 搜索窗口比例（用于性能优化）
     * @return DTW匹配结果
     */
    public DTWMatchResult computeFastDTW(List<Double> sequence1, List<Double> sequence2, 
                                        double maxDistance, double windowRatio) {
        if (sequence1 == null || sequence2 == null || sequence1.isEmpty() || sequence2.isEmpty()) {
            return new DTWMatchResult(Double.MAX_VALUE, 0.0, new ArrayList<>(), 0);
        }

        long startTime = System.currentTimeMillis();

        // 将List转换为数组以提高性能
        double[] seq1 = sequence1.stream().mapToDouble(d -> d).toArray();
        double[] seq2 = sequence2.stream().mapToDouble(d -> d).toArray();

        // 计算FastDTW的近似距离
        double distance = fastDTWInternal(seq1, seq2, windowRatio, maxDistance);
        
        // 计算相似度（将距离转换为0-1范围）
        double maxPossibleDistance = Math.max(getMaxSequenceValue(seq1), getMaxSequenceValue(seq2)) * Math.max(seq1.length, seq2.length);
        double similarity = maxPossibleDistance > 0 ? 1.0 - Math.min(1.0, distance / maxPossibleDistance) : 1.0;

        long endTime = System.currentTimeMillis();

        log.debug("FastDTW计算完成: 距离={}, 相似度={}, 计算时间={}ms", distance, similarity, endTime - startTime);

        return new DTWMatchResult(distance, similarity, new ArrayList<>(), endTime - startTime);
    }

    /**
     * 公共静态FastDTW方法，用于测试和外部调用
     */
    public static double fastDTW(double[] seq1, double[] seq2) {
        DTWSequenceMatcher matcher = new DTWSequenceMatcher();
        return matcher.fastDTW(seq1, seq2, 1.0, Double.MAX_VALUE); // 调用带有 r 和 t 参数的公共方法
    }

    /**
     * FastDTW 重载方法（优化实现）
     */
    public double fastDTW(double[] seq1, double[] seq2, double r, double t) {
        // FastDTW算法实现：多级近似策略加速计算
        if (seq1.length < 2 || seq2.length < 2) {
            // 短序列直接使用DTW
            return computeClassicDTWInternal(seq1, seq2, r, t);
        }

        // 构建多级近似序列
        List<double[]> seq1List = buildPyramid(seq1);
        List<double[]> seq2List = buildPyramid(seq2);

        // 从最粗粒度开始计算
        int finestLevel = seq1List.size() - 1;
        double[] approxSeq1 = seq1List.get(0);
        double[] approxSeq2 = seq2List.get(0);

        // 最粗粒度使用传统DTW计算初始路径
        double distance = computeClassicDTWInternal(approxSeq1, approxSeq2, r, t);

        // 从粗到细逐步优化路径
        for (int level = 1; level <= finestLevel; level++) {
            double[] currentSeq1 = seq1List.get(level);
            double[] currentSeq2 = seq2List.get(level);
            
            // 使用上一层的结果作为初始猜测
            distance = computeClassicDTWInternal(currentSeq1, currentSeq2, r, t);
        }

        return distance;
    }

    /**
     * 构建金字塔近似序列
     */
    private List<double[]> buildPyramid(double[] seq) {
        List<double[]> pyramid = new ArrayList<>();
        pyramid.add(seq);

        // 递归构建下采样序列，直到长度小于10
        while (seq.length > 10) {
            seq = downSample(seq);
            pyramid.add(seq);
        }

        return pyramid;
    }

    /**
     * 下采样：将序列长度减半
     */
    private double[] downSample(double[] seq) {
        int newLength = (seq.length + 1) / 2;
        double[] downSampled = new double[newLength];

        for (int i = 0; i < newLength; i++) {
            int idx1 = 2 * i;
            int idx2 = Math.min(idx1 + 1, seq.length - 1);
            downSampled[i] = (seq[idx1] + seq[idx2]) / 2.0;
        }

        return downSampled;
    }

    /**
     * 内部使用的DTW计算方法
     */
    private double computeClassicDTWInternal(double[] seq1, double[] seq2, double windowRatio, double maxDistance) {
        int n = seq1.length;
        int m = seq2.length;

        // 初始化DTW矩阵 - 只保存两行以节省内存
        double[][] dtw = new double[2][m];
        Arrays.fill(dtw[0], Double.MAX_VALUE);
        Arrays.fill(dtw[1], Double.MAX_VALUE);

        // 设置起始点
        dtw[0][0] = Math.pow(seq1[0] - seq2[0], 2);

        // 计算搜索窗口大小
        int windowSize = (int) (Math.max(n, m) * windowRatio);

        // 填充DTW矩阵
        for (int i = 1; i < n; i++) {
            int currentRow = i % 2;
            int prevRow = (i - 1) % 2;
            Arrays.fill(dtw[currentRow], Double.MAX_VALUE);

            for (int j = Math.max(1, i - windowSize); j < Math.min(m, i + windowSize); j++) {
                double cost = Math.pow(seq1[i] - seq2[j], 2);
                dtw[currentRow][j] = cost + Math.min(
                        Math.min(dtw[prevRow][j], dtw[currentRow][j-1]), 
                        dtw[prevRow][j-1]);

                // 提前终止条件
                if (dtw[currentRow][j] > maxDistance) {
                    return Double.MAX_VALUE;
                }
            }
        }

        return dtw[(n - 1) % 2][m - 1];
    }

    /**
     * FastDTW核心实现 - 优化版
     */
    private double fastDTWInternal(double[] seq1, double[] seq2, double windowRatio, double maxDistance) {
        // 根据序列长度动态调整缩放级别
        int minLength = Math.min(seq1.length, seq2.length);
        int maxScale = Math.max(1, Math.min(4, (int) (Math.log(minLength) / Math.log(2)) - 2));
        
        // 当序列较短时，直接使用传统DTW
        if (minLength < 16) {
            return computeClassicDTWInternal(seq1, seq2, windowRatio, maxDistance);
        }
        
        // 创建缩放后的序列
        List<double[]> scaledSeq1 = createScaledSequences(seq1, maxScale);
        List<double[]> scaledSeq2 = createScaledSequences(seq2, maxScale);
        
        // 从最粗粒度开始计算
        int currentScale = maxScale;
        double[][] dtwCoarse = computeCoarseDTW(scaledSeq1.get(currentScale), scaledSeq2.get(currentScale), windowRatio * 2);
        
        // 逐级细化，使用上一级的结果作为初始窗口
        while (currentScale > 0) {
            currentScale--;
            double[] currentSeq1 = scaledSeq1.get(currentScale);
            double[] currentSeq2 = scaledSeq2.get(currentScale);
            
            // 计算当前级别的搜索窗口
            int window = (int) (Math.max(currentSeq1.length, currentSeq2.length) * windowRatio);
            
            // 使用上一级的结果来限制搜索范围
            dtwCoarse = computeRefinedDTW(currentSeq1, currentSeq2, dtwCoarse, window, maxDistance);
            
            // 检查是否超过最大距离
            if (dtwCoarse[currentSeq1.length-1][currentSeq2.length-1] > maxDistance) {
                return Double.MAX_VALUE;
            }
        }
        
        return dtwCoarse[seq1.length-1][seq2.length-1];
    }
    
    /**
     * 计算两个序列的DTW距离（传统DTW算法）- 直接接受double数组
     */
    private DTWMatchResult computeClassicDTW(double[] seq1, double[] seq2, double maxDistance, double windowRatio) {
        if (seq1 == null || seq2 == null || seq1.length == 0 || seq2.length == 0) {
            return new DTWMatchResult(Double.MAX_VALUE, 0.0, new ArrayList<>(), 0);
        }

        long startTime = System.currentTimeMillis();

        int n = seq1.length;
        int m = seq2.length;

        // 初始化DTW矩阵 - 只保存两行以节省内存
        double[][] dtw = new double[2][m];
        Arrays.fill(dtw[0], Double.MAX_VALUE);
        Arrays.fill(dtw[1], Double.MAX_VALUE);

        // 设置起始点
        dtw[0][0] = Math.pow(seq1[0] - seq2[0], 2);

        // 计算搜索窗口大小
        int windowSize = (int) (Math.max(n, m) * windowRatio);

        // 填充DTW矩阵
        for (int i = 1; i < n; i++) {
            // 计算当前行和前一行的索引
            int currentRow = i % 2;
            int prevRow = (i - 1) % 2;
            
            // 重置当前行
            Arrays.fill(dtw[currentRow], Double.MAX_VALUE);
            
            for (int j = Math.max(1, i - windowSize); j < Math.min(m, i + windowSize); j++) {
                double cost = Math.pow(seq1[i] - seq2[j], 2);
                dtw[currentRow][j] = cost + Math.min(Math.min(dtw[prevRow][j], dtw[currentRow][j-1]), dtw[prevRow][j-1]);

                // 提前终止条件
                if (dtw[currentRow][j] > maxDistance) {
                    long endTime = System.currentTimeMillis();
                    return new DTWMatchResult(Double.MAX_VALUE, 0.0, new ArrayList<>(), endTime - startTime);
                }
            }
        }

        // 计算相似度（将距离转换为0-1范围）
        double distance = dtw[(n - 1) % 2][m - 1];
        double maxPossibleDistance = Math.max(getMaxSequenceValue(seq1), getMaxSequenceValue(seq2)) * Math.max(n, m);
        double similarity = maxPossibleDistance > 0 ? 1.0 - Math.min(1.0, distance / maxPossibleDistance) : 1.0;

        long endTime = System.currentTimeMillis();

        log.debug("DTW计算完成: 距离={}, 相似度={}, 计算时间={}ms", distance, similarity, endTime - startTime);

        return new DTWMatchResult(distance, similarity, new ArrayList<>(), endTime - startTime);
    }

    /**
     * 创建缩放后的序列列表（多级近似）
     */
    private List<double[]> createScaledSequences(double[] sequence, int maxScale) {
        List<double[]> scaledSequences = new ArrayList<>();
        scaledSequences.add(sequence);
        
        double[] currentSequence = sequence;
        for (int scale = 1; scale <= maxScale; scale++) {
            // 下采样：每两个点取一个平均值
            int newLength = (currentSequence.length + 1) / 2;
            double[] newSequence = new double[newLength];
            
            for (int i = 0; i < newLength; i++) {
                int index1 = i * 2;
                int index2 = Math.min(i * 2 + 1, currentSequence.length - 1);
                newSequence[i] = (currentSequence[index1] + currentSequence[index2]) / 2.0;
            }
            
            scaledSequences.add(newSequence);
            currentSequence = newSequence;
        }
        
        return scaledSequences;
    }

    /**
     * 计算粗粒度DTW
     */
    private double[][] computeCoarseDTW(double[] seq1, double[] seq2, double windowRatio) {
        int n = seq1.length;
        int m = seq2.length;
        int window = (int) (Math.max(n, m) * windowRatio);
        
        double[][] dtw = new double[n][m];
        for (int i = 0; i < n; i++) {
            Arrays.fill(dtw[i], Double.MAX_VALUE);
        }
        
        dtw[0][0] = Math.pow(seq1[0] - seq2[0], 2);
        
        for (int i = 1; i < n; i++) {
            for (int j = Math.max(1, i - window); j < Math.min(m, i + window); j++) {
                double cost = Math.pow(seq1[i] - seq2[j], 2);
                dtw[i][j] = cost + Math.min(Math.min(dtw[i-1][j], dtw[i][j-1]), dtw[i-1][j-1]);
            }
        }
        
        return dtw;
    }

    /**
     * 计算细化DTW
     */
    private double[][] computeRefinedDTW(double[] seq1, double[] seq2, double[][] coarseDTW, 
                                        int window, double maxDistance) {
        int n = seq1.length;
        int m = seq2.length;
        int coarseN = coarseDTW.length;
        int coarseM = coarseDTW[0].length;
        
        double[][] dtw = new double[n][m];
        for (int i = 0; i < n; i++) {
            Arrays.fill(dtw[i], Double.MAX_VALUE);
        }
        
        dtw[0][0] = Math.pow(seq1[0] - seq2[0], 2);
        
        for (int i = 1; i < n; i++) {
            // 计算粗粒度对应位置
            int coarseI = Math.min(coarseN - 1, i / 2);
            
            for (int j = 1; j < m; j++) {
                // 计算粗粒度对应位置
                int coarseJ = Math.min(coarseM - 1, j / 2);
                
                // 检查是否在搜索窗口内
                if (Math.abs(i - j) > window) {
                    continue;
                }
                
                double cost = Math.pow(seq1[i] - seq2[j], 2);
                dtw[i][j] = cost + Math.min(Math.min(dtw[i-1][j], dtw[i][j-1]), dtw[i-1][j-1]);
                
                // 提前终止条件
                if (dtw[i][j] > maxDistance) {
                    dtw[i][j] = Double.MAX_VALUE;
                }
            }
        }
        
        return dtw;
    }

    /**
     * 带窗口约束的对齐路径计算
     */
    private List<int[]> getAlignmentPathWithWindow(double[] seq1, double[] seq2, int windowSize, double maxDistance) {
        int n = seq1.length;
        int m = seq2.length;
        
        // 重新计算完整的DTW矩阵以获取路径
        double[][] dtw = new double[n][m];
        for (int i = 0; i < n; i++) {
            Arrays.fill(dtw[i], Double.MAX_VALUE);
        }
        
        dtw[0][0] = Math.pow(seq1[0] - seq2[0], 2);
        
        for (int i = 1; i < n; i++) {
            for (int j = Math.max(1, i - windowSize); j < Math.min(m, i + windowSize); j++) {
                double cost = Math.pow(seq1[i] - seq2[j], 2);
                dtw[i][j] = cost + Math.min(Math.min(dtw[i-1][j], dtw[i][j-1]), dtw[i-1][j-1]);
            }
        }
        
        return getAlignmentPath(dtw);
    }

    /**
     * 从K线数据中提取技术指标序列
     * @param klines K线数据列表
     * @param indicatorType 技术指标类型
     * @param period 指标周期
     * @return 技术指标序列
     */
    public List<Double> extractIndicatorSequence(List<Kline> klines, IndicatorType indicatorType, int period) {
        switch (indicatorType) {
            case MA:
                return IndicatorCalculator.calculateMA(klines, period).stream()
                        .map(Double::valueOf)
                        .collect(Collectors.toList());
            case EMA:
                return IndicatorCalculator.calculateEMA(klines, period).stream()
                        .map(Double::valueOf)
                        .collect(Collectors.toList());
            case RSI:
                return IndicatorCalculator.calculateRSI(klines, period);
            case MACD:
                return IndicatorCalculator.calculateMACD(klines, period, period * 2, period / 2).getMacd();
            case BOLL_UPPER:
                return IndicatorCalculator.calculateBOLL(klines, period, 2).getUpper().stream()
                        .map(Double::valueOf)
                        .collect(Collectors.toList());
            case BOLL_MIDDLE:
                return IndicatorCalculator.calculateBOLL(klines, period, 2).getMiddle().stream()
                        .map(Double::valueOf)
                        .collect(Collectors.toList());
            case BOLL_LOWER:
                return IndicatorCalculator.calculateBOLL(klines, period, 2).getLower().stream()
                        .map(Double::valueOf)
                        .collect(Collectors.toList());
            case CLOSE_PRICE:
                return klines.stream()
                        .map(k -> Double.valueOf(k.getClose()))
                        .collect(Collectors.toList());
            default:
                throw new IllegalArgumentException("不支持的技术指标类型: " + indicatorType);
        }
    }

    /**
     * 标准化序列（归一化到0-1范围）
     * @param sequence 原始序列
     * @return 标准化后的序列
     */
    public List<Double> normalizeSequence(List<Double> sequence) {
        if (sequence == null || sequence.isEmpty()) {
            return new ArrayList<>();
        }

        double min = sequence.stream().min(Double::compare).orElse(0.0);
        double max = sequence.stream().max(Double::compare).orElse(1.0);
        double range = max - min;

        if (range == 0) {
            return sequence.stream().map(d -> 0.0).collect(Collectors.toList());
        }

        return sequence.stream()
                .map(d -> (d - min) / range)
                .collect(Collectors.toList());
    }

    /**
     * 平滑序列（减少噪声）
     * @param sequence 原始序列
     * @param windowSize 平滑窗口大小
     * @return 平滑后的序列
     */
    public List<Double> smoothSequence(List<Double> sequence, int windowSize) {
        if (sequence == null || sequence.isEmpty() || windowSize <= 1) {
            return new ArrayList<>(sequence);
        }

        List<Double> smoothed = new ArrayList<>();
        for (int i = 0; i < sequence.size(); i++) {
            int start = Math.max(0, i - windowSize / 2);
            int end = Math.min(sequence.size(), i + windowSize / 2 + 1);
            double sum = 0.0;
            for (int j = start; j < end; j++) {
                sum += sequence.get(j);
            }
            smoothed.add(sum / (end - start));
        }

        return smoothed;
    }

    /**
     * 获取序列的最大值（用于相似度计算）
     */
    private double getMaxSequenceValue(double[] sequence) {
        double max = 0.0;
        for (double value : sequence) {
            max = Math.max(max, Math.abs(value));
        }
        return max;
    }

    /**
     * 回溯获取DTW对齐路径
     */
    private List<int[]> getAlignmentPath(double[][] dtw) {
        List<int[]> path = new ArrayList<>();
        int i = dtw.length - 1;
        int j = dtw[0].length - 1;

        path.add(new int[]{i, j});

        while (i > 0 || j > 0) {
            if (i == 0) {
                j--;
            } else if (j == 0) {
                i--;
            } else {
                double minVal = Math.min(Math.min(dtw[i-1][j], dtw[i][j-1]), dtw[i-1][j-1]);
                if (minVal == dtw[i-1][j-1]) {
                    i--;
                    j--;
                } else if (minVal == dtw[i-1][j]) {
                    i--;
                } else {
                    j--;
                }
            }
            path.add(new int[]{i, j});
        }

        // 反转路径，使其从起点开始
        List<int[]> reversedPath = new ArrayList<>();
        for (int k = path.size() - 1; k >= 0; k--) {
            reversedPath.add(path.get(k));
        }

        return reversedPath;
    }

    /**
     * 技术指标类型枚举
     */
    public enum IndicatorType {
        MA,         // 移动平均线
        EMA,        // 指数移动平均线
        RSI,        // 相对强弱指标
        MACD,       // 平滑异同移动平均线
        BOLL_UPPER, // 布林带上轨
        BOLL_MIDDLE, // 布林带中轨
        BOLL_LOWER, // 布林带下轨
        CLOSE_PRICE // 收盘价
    }

    /**
     * 批量比较序列相似度
     * @param targetSequence 目标序列
     * @param candidateSequences 候选序列列表
     * @return 相似度排序结果
     */
    public List<DTWMatchResult> batchCompareSequences(List<Double> targetSequence, 
                                                      List<List<Double>> candidateSequences) {
        return candidateSequences.parallelStream()
                .map(candidate -> computeDTW(targetSequence, candidate))
                .sorted((r1, r2) -> Double.compare(r1.getDistance(), r2.getDistance()))
                .collect(Collectors.toList());
    }

    /**
     * 验证DTW参数有效性
     * @param sequence1 第一个序列
     * @param sequence2 第二个序列
     * @return 参数是否有效
     */
    public boolean validateDTWParameters(List<Double> sequence1, List<Double> sequence2) {
        if (sequence1 == null || sequence1.isEmpty() || sequence2 == null || sequence2.isEmpty()) {
            log.error("DTW参数无效: 序列不能为空");
            return false;
        }

        if (sequence1.size() > 1000 || sequence2.size() > 1000) {
            log.warn("DTW序列过长，可能导致性能问题: {}/{}个点", sequence1.size(), sequence2.size());
        }

        return true;
    }
}
