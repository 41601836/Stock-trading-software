package com.stock.analysis.module.chip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.analysis.common.exception.BusinessException;
import com.stock.analysis.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicDouble;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChipAnalysisServiceImpl implements ChipAnalysisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 时间衰减系数 (lambda)，值越大近期权重越高
    private static final double DECAY_LAMBDA = 0.05;
    // 精细时间衰减因子 (小时级)
    private static final double HOURLY_DECAY_LAMBDA = 0.001;
    // 分钟级时间衰减因子
    private static final double MINUTE_DECAY_LAMBDA = 0.00002;
    // 成交量加权因子
    private static final double VOLUME_WEIGHT_FACTOR = 0.00001;
    // 模拟分析的天数
    private static final int ANALYSIS_DAYS = 60;
    // 缓存前缀
    private static final String CACHE_KEY_PREFIX = "chip:dist:";
    // 支撑压力位识别的价格区间阈值
    private static final double SUPPORT_RESISTANCE_THRESHOLD = 1.5;
    // 筹码集中度计算的窗口大小
    private static final int CONCENTRATION_WINDOW_SIZE = 5;

    @Override
    public StockChip calculateDistribution(String stockCode) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 1. 尝试从 Redis 获取缓存
        String cacheKey = CACHE_KEY_PREFIX + stockCode;
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("Cache Hit for stock: {}", stockCode);
                return objectMapper.readValue(cachedJson, StockChip.class);
            }
        } catch (Exception e) {
            log.warn("Redis read failed: {}", e.getMessage());
        }

        // 2. 获取历史交易数据 (Mock)
        List<TradeRecord> historyData = generateMockHistory(stockCode, ANALYSIS_DAYS);

        // 3. WAD 算法计算 - 使用并行处理提高性能
        // Map<价格(分), 加权筹码量>
        Map<Integer, Double> chipDistribution = new ConcurrentHashMap<>();
        AtomicDouble totalWeightedVolume = new AtomicDouble(0);

        LocalDateTime now = LocalDateTime.now();

        // 并行处理历史数据
        historyData.parallelStream().forEach(record -> {
            // 计算精确时间差（到分钟）
            long minutesDiff = java.time.Duration.between(record.tradeTime, now).toMinutes();
            if (minutesDiff < 0) minutesDiff = 0;

            // 精细指数衰减公式: Weight = Volume * e^(-(lambda_day * days + lambda_hour * hours + lambda_minute * minutes))
            long daysDiff = minutesDiff / (24 * 60);
            long remainingMinutes = minutesDiff % (24 * 60);
            long hoursDiff = remainingMinutes / 60;
            long minutes = remainingMinutes % 60;
            
            // 计算衰减因子
            double decayFactor = Math.exp(
                -DECAY_LAMBDA * daysDiff 
                - HOURLY_DECAY_LAMBDA * hoursDiff 
                - MINUTE_DECAY_LAMBDA * minutes
            );
            
            // 成交量加权 - 成交量越大，权重越高
            double volumeWeight = Math.exp(VOLUME_WEIGHT_FACTOR * record.volume);
            double weightedVolume = record.volume * decayFactor * volumeWeight;

            // 将筹码累加到对应价格
            chipDistribution.merge(record.avgPrice, weightedVolume, Double::sum);
            totalWeightedVolume.addAndGet(weightedVolume);
        });

        if (totalWeightedVolume.get() <= 0) {
            throw new BusinessException(ErrorCode.NO_HEAT_DATA);
        }

        // 4. 计算统计指标
        // 4.1 主筹成本 (加权平均价格)
        double sumProduct = 0;
        List<Map.Entry<Integer, Double>> sortedChips = new ArrayList<>(chipDistribution.entrySet());
        // 按价格排序
        sortedChips.sort(Map.Entry.comparingByKey());

        for (Map.Entry<Integer, Double> entry : sortedChips) {
            sumProduct += entry.getKey() * entry.getValue();
        }
        int mainCost = (int) (sumProduct / totalWeightedVolume);

        // 4.2 计算筹码集中度 (90% 筹码分布区间)
        double currentSum = 0;
        int priceRangeLow = sortedChips.get(0).getKey();
        int priceRangeHigh = sortedChips.get(sortedChips.size() - 1).getKey();
        boolean foundLow = false;

        for (Map.Entry<Integer, Double> entry : sortedChips) {
            currentSum += entry.getValue();
            double ratio = currentSum / totalWeightedVolume;

            if (!foundLow && ratio >= 0.05) {
                priceRangeLow = entry.getKey();
                foundLow = true;
            }
            if (ratio >= 0.95) {
                priceRangeHigh = entry.getKey();
                break;
            }
        }

        // 4.3 计算该区间内的实际占比 - 使用并行处理
        double rangeVolume = sortedChips.parallelStream()
                .filter(entry -> entry.getKey() >= priceRangeLow && entry.getKey() <= priceRangeHigh)
                .mapToDouble(Map.Entry::getValue)
                .sum();
        BigDecimal chipRatio = BigDecimal.valueOf(rangeVolume / totalWeightedVolume.get())
                .setScale(4, RoundingMode.HALF_UP);

        // 4.4 集中度计算公式: (High - Low) / (High + Low)
        double concentration = (double) (priceRangeHigh - priceRangeLow) / (priceRangeHigh + priceRangeLow);
        BigDecimal concentrationRatio = BigDecimal.valueOf(concentration).setScale(4, RoundingMode.HALF_UP);

        // 4.5 计算主筹峰值 (筹码最多的价格)
        // 使用并行流查找最大值提高性能
        Optional<Map.Entry<Integer, Double>> maxEntry = sortedChips.parallelStream()
                .max(Map.Entry.comparingByValue());
        int peakPrice = maxEntry.map(Map.Entry::getKey).orElse(0);
        double maxVolume = maxEntry.map(Map.Entry::getValue).orElse(0.0);

        // 4.6 计算支撑位和压力位
        // 支撑位：筹码密集区的下限，或当前价格下方的筹码峰
        // 压力位：筹码密集区的上限，或当前价格上方的筹码峰
        int supportLevel = priceRangeLow;
        int resistanceLevel = priceRangeHigh;
        
        // 获取当前价格
        int currentPrice = historyData.get(historyData.size() - 1).closePrice;
        
        // 寻找所有局部峰值
        List<Map.Entry<Integer, Double>> localPeaks = findLocalPeaks(sortedChips, CONCENTRATION_WINDOW_SIZE, SUPPORT_RESISTANCE_THRESHOLD);
        
        // 寻找当前价格下方的主要支撑位
        double supportPeakVolume = 0;
        for (Map.Entry<Integer, Double> peak : localPeaks) {
            int price = peak.getKey();
            double volume = peak.getValue();
            
            if (price < currentPrice && volume > supportPeakVolume) {
                supportPeakVolume = volume;
                supportLevel = price;
            }
        }
        
        // 寻找当前价格上方的主要压力位
        double resistancePeakVolume = 0;
        for (Map.Entry<Integer, Double> peak : localPeaks) {
            int price = peak.getKey();
            double volume = peak.getValue();
            
            if (price > currentPrice && volume > resistancePeakVolume) {
                resistancePeakVolume = volume;
                resistanceLevel = price;
            }
        }
        
        // 如果没有找到明显的支撑/压力位，使用筹码分布的重心位置
        if (supportLevel == priceRangeLow && supportPeakVolume == 0) {
            supportLevel = calculateDistributionCenter(chipDistribution, false);
        }
        if (resistanceLevel == priceRangeHigh && resistancePeakVolume == 0) {
            resistanceLevel = calculateDistributionCenter(chipDistribution, true);
        }

        StockChip result = StockChip.builder()
                .stockCode(stockCode)
                .analyzeTime(now)
                .currentPrice(currentPrice)
                .mainCost(mainCost)
                .priceRangeLow(priceRangeLow)
                .priceRangeHigh(priceRangeHigh)
                .chipRatio(chipRatio)
                .concentrationRatio(concentrationRatio)
                .peakPrice(peakPrice)
                .supportLevel(supportLevel)
                .resistanceLevel(resistanceLevel)
                .detailedDistribution(chipDistribution)
                .createTime(now)
                .updateTime(now)
                .build();

        // 5. 写入 Redis 缓存 (5秒过期)
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis write failed: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Mock 数据生成器
     */
    private List<TradeRecord> generateMockHistory(String stockCode, int days) {
        List<TradeRecord> records = new ArrayList<>();
        LocalDateTime endTime = LocalDateTime.now();
        Random random = new Random();

        // 初始价格 10.00 元 (1000分)
        int basePrice = 1000; 
        
        for (int i = days; i >= 0; i--) {
            LocalDateTime tradeTime = endTime.minusDays(i);
            
            // 模拟价格波动 (正弦趋势 + 随机扰动)
            double trend = Math.sin(i * 0.1) * 100; // 波动幅度 1元
            int noise = random.nextInt(50) - 25; // 随机 -0.25 ~ +0.25
            
            int closePrice = basePrice + (int) trend + noise;
            if (closePrice <= 0) closePrice = 100; // 兜底

            // 模拟成交量 (价格高时量可能大，也可能小，这里随机)
            long volume = 1000000L + random.nextInt(5000000); // 100万 - 600万股

            // 均价 (简化为收盘价附近)
            int avgPrice = closePrice + (random.nextInt(20) - 10);

            records.add(new TradeRecord(tradeTime, closePrice, avgPrice, volume));
        }
        return records;
    }

    /**
     * 查找筹码分布的局部峰值
     */
    private List<Map.Entry<Integer, Double>> findLocalPeaks(List<Map.Entry<Integer, Double>> sortedChips, int windowSize, double threshold) {
        List<Map.Entry<Integer, Double>> peaks = new ArrayList<>();
        int n = sortedChips.size();
        
        for (int i = 0; i < n; i++) {
            Map.Entry<Integer, Double> current = sortedChips.get(i);
            boolean isPeak = true;
            
            // 检查窗口内的所有点
            for (int j = Math.max(0, i - windowSize); j <= Math.min(n - 1, i + windowSize); j++) {
                if (j != i) {
                    Map.Entry<Integer, Double> neighbor = sortedChips.get(j);
                    if (neighbor.getValue() > current.getValue() * threshold) {
                        isPeak = false;
                        break;
                    }
                }
            }
            
            if (isPeak) {
                peaks.add(current);
            }
        }
        
        return peaks;
    }
    
    /**
     * 计算筹码分布的重心位置
     */
    private int calculateDistributionCenter(Map<Integer, Double> chipDistribution, boolean isResistance) {
        double totalWeight = 0;
        double weightedSum = 0;
        
        // 计算加权和
        for (Map.Entry<Integer, Double> entry : chipDistribution.entrySet()) {
            int price = entry.getKey();
            double weight = entry.getValue();
            totalWeight += weight;
            weightedSum += price * weight;
        }
        
        if (totalWeight == 0) {
            return 0;
        }
        
        int center = (int) (weightedSum / totalWeight);
        
        // 对于阻力位，取重心上方的密集区；对于支撑位，取重心下方的密集区
        if (isResistance) {
            for (Map.Entry<Integer, Double> entry : chipDistribution.entrySet()) {
                if (entry.getKey() > center && entry.getValue() > chipDistribution.getOrDefault(center, 0.0) * 0.8) {
                    center = Math.max(center, entry.getKey());
                }
            }
        } else {
            for (Map.Entry<Integer, Double> entry : chipDistribution.entrySet()) {
                if (entry.getKey() < center && entry.getValue() > chipDistribution.getOrDefault(center, 0.0) * 0.8) {
                    center = Math.min(center, entry.getKey());
                }
            }
        }
        
        return center;
    }

    /**
     * 内部类：交易记录
     */
    private record TradeRecord(LocalDateTime tradeTime, int closePrice, int avgPrice, long volume) {}
}
