package com.stock.analysis.module.tech;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * K线形态识别器 (CNN模型接入层)
 * 支持常见K线形态识别，为CNN模型提供标准化接口
 */
@Slf4j
@Component
public class KlinePatternRecognizer {

    // 支持的K线形态类型
    public enum PatternType {
        DOUBLE_BOTTOM,     // 双底
        DOUBLE_TOP,        // 双顶
        HEAD_SHOULDERS,    // 头肩顶
        INVERTED_HEAD_SHOULDERS, // 倒头肩底
        TRIANGLE_BREAKOUT, // 三角形突破
        FLAG_PATTERN,      // 旗形
        PENNANT_PATTERN,   // 楔形
        ROCKET_PATTERN,    // 火箭形态（暴涨模型）
        NO_PATTERN         // 无明显形态
    }

    // 形态识别结果类
    public static class PatternRecognitionResult {
        private final PatternType patternType;
        private final double confidence; // 识别置信度 (0-1)
        private final String patternName; // 形态名称
        private final Map<String, Object> details; // 详细信息

        public PatternRecognitionResult(PatternType patternType, double confidence, Map<String, Object> details) {
            this.patternType = patternType;
            this.confidence = confidence;
            this.patternName = getPatternName(patternType);
            this.details = details;
        }

        public PatternType getPatternType() { return patternType; }
        public double getConfidence() { return confidence; }
        public String getPatternName() { return patternName; }
        public Map<String, Object> getDetails() { return details; }
    }

    /**
     * 初始化CNN模型
     * 实际项目中，这里会加载预训练的CNN模型
     */
    public void initializeModel() {
        log.info("初始化K线形态识别CNN模型...");
        // 模拟加载模型
        try {
            Thread.sleep(100);
            log.info("K线形态识别CNN模型初始化完成");
        } catch (InterruptedException e) {
            log.error("模型加载中断", e);
        }
    }

    /**
     * 识别K线形态
     * @param klines K线数据列表
     * @return 形态识别结果
     */
    public PatternRecognitionResult recognizePattern(List<Kline> klines) {
        if (klines == null || klines.size() < 20) {
            return new PatternRecognitionResult(PatternType.NO_PATTERN, 0.0, Collections.emptyMap());
        }

        log.debug("识别K线形态，共{}根K线", klines.size());

        // 1. 标准化K线数据（用于CNN输入）
        List<double[]> normalizedData = normalizeKlineData(klines);

        // 2. 提取技术特征（作为CNN输入的一部分）
        Map<String, double[]> technicalFeatures = extractTechnicalFeatures(klines);

        // 3. 调用CNN模型进行识别
        // 实际项目中，这里会将数据输入到预训练的CNN模型
        PatternRecognitionResult result = mockCNNInference(normalizedData, technicalFeatures);

        log.debug("形态识别完成: {} (置信度: {:.2f})");

        return result;
    }

    /**
     * 标准化K线数据
     * 将K线数据转换为CNN模型可接受的格式
     */
    private List<double[]> normalizeKlineData(List<Kline> klines) {
        // 提取收盘价序列
        List<Integer> closePrices = klines.stream().map(Kline::getClose).collect(Collectors.toList());

        // 计算最高价和最低价（用于归一化）
        int maxPrice = closePrices.stream().max(Integer::compareTo).orElse(1);
        int minPrice = closePrices.stream().min(Integer::compareTo).orElse(0);
        double priceRange = maxPrice - minPrice;
        if (priceRange == 0) priceRange = 1;

        // 计算最大成交量（用于归一化） - 只计算一次
        long maxVolume = klines.stream().map(Kline::getVolume).max(Long::compareTo).orElse(1L);
        final double finalMaxVolume = (double) maxVolume;

        // 归一化K线数据 (OHLCV)
        return klines.parallelStream().map(kline -> {
            double[] normalized = new double[5];
            normalized[0] = (kline.getOpen() - minPrice) / priceRange;
            normalized[1] = (kline.getHigh() - minPrice) / priceRange;
            normalized[2] = (kline.getLow() - minPrice) / priceRange;
            normalized[3] = (kline.getClose() - minPrice) / priceRange;
            normalized[4] = (double) kline.getVolume() / finalMaxVolume;
            return normalized;
        }).collect(Collectors.toList());
    }

    /**
     * 提取技术特征
     */
    private Map<String, double[]> extractTechnicalFeatures(List<Kline> klines) {
        Map<String, double[]> features = new HashMap<>();

        // 计算MA指标
        List<Integer> ma5 = IndicatorCalculator.calculateMA(klines, 5);
        List<Integer> ma10 = IndicatorCalculator.calculateMA(klines, 10);
        List<Integer> ma20 = IndicatorCalculator.calculateMA(klines, 20);

        // 计算RSI指标
        List<Double> rsi14 = IndicatorCalculator.calculateRSI(klines, 14);

        // 计算MACD指标
        IndicatorCalculator.MACDResult macd = IndicatorCalculator.calculateMACD(klines, 12, 26, 9);

        // 计算布林带指标
        IndicatorCalculator.BOLLResult boll = IndicatorCalculator.calculateBOLL(klines, 20, 2);

        // 将指标转换为特征数组
        features.put("MA5", ma5.stream().mapToDouble(i -> i).toArray());
        features.put("MA10", ma10.stream().mapToDouble(i -> i).toArray());
        features.put("MA20", ma20.stream().mapToDouble(i -> i).toArray());
        features.put("RSI14", rsi14.stream().mapToDouble(d -> d).toArray());
        features.put("MACD", macd.getMacd().stream().mapToDouble(d -> d).toArray());
        features.put("MACD_SIGNAL", macd.getSignal().stream().mapToDouble(d -> d).toArray());
        features.put("BOLL_UPPER", boll.getUpper().stream().mapToDouble(i -> i).toArray());
        features.put("BOLL_MIDDLE", boll.getMiddle().stream().mapToDouble(i -> i).toArray());
        features.put("BOLL_LOWER", boll.getLower().stream().mapToDouble(i -> i).toArray());

        return features;
    }

    /**
     * 模拟CNN模型推理
     * 实际项目中，这里会调用真实的CNN模型
     */
    private PatternRecognitionResult mockCNNInference(List<double[]> normalizedData, Map<String, double[]> technicalFeatures) {
        Random random = new Random();
        
        // 随机选择一种形态（实际项目中由CNN模型决定）
        PatternType[] patterns = Arrays.stream(PatternType.values())
                .filter(type -> type != PatternType.NO_PATTERN)
                .toArray(PatternType[]::new);
        
        PatternType selectedPattern = patterns[random.nextInt(patterns.length)];
        double confidence = 0.7 + random.nextDouble() * 0.3; // 70%-100%置信度

        // 生成详细信息
        Map<String, Object> details = new HashMap<>();
        details.put("patternLength", normalizedData.size());
        details.put("timestamp", System.currentTimeMillis());
        details.put("technicalFeatures", extractFeatureSummary(technicalFeatures));

        // 30%概率返回无形态
        if (random.nextDouble() < 0.3) {
            return new PatternRecognitionResult(PatternType.NO_PATTERN, 1.0, Collections.emptyMap());
        }

        return new PatternRecognitionResult(selectedPattern, confidence, details);
    }

    /**
     * 提取特征摘要（用于结果展示）
     */
    private Map<String, Double> extractFeatureSummary(Map<String, double[]> technicalFeatures) {
        Map<String, Double> summary = new HashMap<>();
        
        for (Map.Entry<String, double[]> entry : technicalFeatures.entrySet()) {
            double[] values = entry.getValue();
            if (values.length > 0) {
                // 取最新的特征值
                summary.put(entry.getKey(), values[values.length - 1]);
            }
        }
        
        return summary;
    }

    /**
     * 获取形态名称
     */
    private static String getPatternName(PatternType patternType) {
        Map<PatternType, String> patternNames = new EnumMap<>(PatternType.class);
        patternNames.put(PatternType.DOUBLE_BOTTOM, "双底形态");
        patternNames.put(PatternType.DOUBLE_TOP, "双顶形态");
        patternNames.put(PatternType.HEAD_SHOULDERS, "头肩顶");
        patternNames.put(PatternType.INVERTED_HEAD_SHOULDERS, "倒头肩底");
        patternNames.put(PatternType.TRIANGLE_BREAKOUT, "三角形突破");
        patternNames.put(PatternType.FLAG_PATTERN, "旗形形态");
        patternNames.put(PatternType.PENNANT_PATTERN, "楔形形态");
        patternNames.put(PatternType.ROCKET_PATTERN, "火箭形态");
        patternNames.put(PatternType.NO_PATTERN, "无明显形态");
        
        return patternNames.getOrDefault(patternType, "未知形态");
    }

    /**
     * 获取标准化的CNN输入数据
     * 为外部CNN模型提供标准化的输入格式
     */
    public double[][][] getCNNInput(List<Kline> klines, int inputLength) {
        // 确保K线数据足够长
        if (klines.size() < inputLength) {
            throw new IllegalArgumentException("K线数据不足，需要至少" + inputLength + "根K线");
        }

        // 截取最新的K线数据
        List<Kline> recentKlines = klines.subList(Math.max(0, klines.size() - inputLength), klines.size());

        // 归一化数据
        List<double[]> normalizedData = normalizeKlineData(recentKlines);

        // 转换为CNN输入格式 [batch_size, sequence_length, feature_dimension]
        double[][][] input = new double[1][inputLength][5]; // OHLCV 5个特征
        
        for (int i = 0; i < recentKlines.size(); i++) {
            input[0][i] = normalizedData.get(i);
        }

        return input;
    }
}
