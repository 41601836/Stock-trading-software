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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChipAnalysisServiceImpl implements ChipAnalysisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 时间衰减系数 (lambda)，值越大近期权重越高
    private static final double DECAY_LAMBDA = 0.05;
    // 模拟分析的天数
    private static final int ANALYSIS_DAYS = 60;
    // 缓存前缀
    private static final String CACHE_KEY_PREFIX = "chip:dist:";

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

        // 3. WAD 算法计算
        // Map<价格(分), 加权筹码量>
        Map<Integer, Double> chipDistribution = new HashMap<>();
        double totalWeightedVolume = 0;

        LocalDateTime now = LocalDateTime.now();

        for (TradeRecord record : historyData) {
            // 计算距今天数
            long daysDiff = java.time.Duration.between(record.tradeTime, now).toDays();
            if (daysDiff < 0) daysDiff = 0;

            // 指数衰减公式: Weight = Volume * e^(-lambda * days)
            double decayFactor = Math.exp(-DECAY_LAMBDA * daysDiff);
            double weightedVolume = record.volume * decayFactor;

            // 将筹码累加到对应价格
            chipDistribution.merge(record.avgPrice, weightedVolume, Double::sum);
            totalWeightedVolume += weightedVolume;
        }

        if (totalWeightedVolume == 0) {
            throw new BusinessException(ErrorCode.NO_HEAT_DATA);
        }

        // 4. 计算统计指标
        // 4.1 主筹成本 (加权平均价格)
        double sumProduct = 0;
        List<Map.Entry<Integer, Double>> sortedChips = new ArrayList<>(chipDistribution.entrySet());
        // 按价格排序
        sortedChips.sort(Map.Entry.comparingByKey());

        for (Map.Entry<Integer, Double> entry : sortedChips) {
            // 精度检查: entry.getKey() 是 Integer(分), entry.getValue() 是 Double(量)
            // 乘积为 Double，累加后除以总量，结果强转 int，符合逻辑
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

        // 4.3 计算该区间内的实际占比
        double rangeVolume = 0;
        for (Map.Entry<Integer, Double> entry : sortedChips) {
            if (entry.getKey() >= priceRangeLow && entry.getKey() <= priceRangeHigh) {
                rangeVolume += entry.getValue();
            }
        }
        BigDecimal chipRatio = BigDecimal.valueOf(rangeVolume / totalWeightedVolume)
                .setScale(4, RoundingMode.HALF_UP);

        // 4.4 集中度计算公式: (High - Low) / (High + Low)
        // 精度检查: 分子分母均为 int，先转 double 再除，避免整数除法为 0
        double concentration = (double) (priceRangeHigh - priceRangeLow) / (priceRangeHigh + priceRangeLow);
        BigDecimal concentrationRatio = BigDecimal.valueOf(concentration).setScale(4, RoundingMode.HALF_UP);

        // 获取当前价格
        int currentPrice = historyData.get(historyData.size() - 1).closePrice;

        StockChip result = StockChip.builder()
                .stockCode(stockCode)
                .analyzeTime(now)
                .currentPrice(currentPrice)
                .mainCost(mainCost)
                .priceRangeLow(priceRangeLow)
                .priceRangeHigh(priceRangeHigh)
                .chipRatio(chipRatio)
                .concentrationRatio(concentrationRatio)
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
     * 内部类：交易记录
     */
    private record TradeRecord(LocalDateTime tradeTime, int closePrice, int avgPrice, long volume) {}
}
