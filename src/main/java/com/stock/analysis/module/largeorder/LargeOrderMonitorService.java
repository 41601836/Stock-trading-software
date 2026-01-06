package com.stock.analysis.module.largeorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Random;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LargeOrderMonitorService {

    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;
    private static final String CACHE_KEY_PREFIX = "largeorder:netinflow:";
    private static final String THRESHOLD_CACHE_PREFIX = "largeorder:threshold:";
    
    // 动态阈值参数
    private static final int RECENT_TRADES_SIZE = 1000; // 用于计算动态阈值的近期交易数量
    private static final double STANDARD_DEVIATION_MULTIPLE = 3.0; // 均值 + N倍标准差
    private static final int THRESHOLD_CACHE_SECONDS = 60; // 阈值缓存时间
    
    // 按股票代码存储近期交易数据 - 使用环形数组提高性能
    private final long[] recentTradeAmounts = new long[RECENT_TRADES_SIZE];
    private final AtomicInteger recentTradeIndex = new AtomicInteger(0);
    private final AtomicInteger recentTradeCount = new AtomicInteger(0);

    /**
     * 模拟接收 Level-2 实时成交流
     * 实际场景中，这里可能是 Kafka 消费者或 TCP Socket 回调
     */
    public void processRealTimeStream(String stockCode) {
        // 模拟生成一笔交易数据
        TradeData trade = mockLevel2Data(stockCode);

        // 监控逻辑: 判断是否为大单
        long threshold = calculateDynamicThreshold(stockCode);
        
        // 记录当前交易金额用于动态阈值计算
        updateRecentTrades(trade.amount);
        
        if (trade.amount > threshold) {
            // 识别意图
            String intent = identifyIntent(trade.amount, threshold, trade.direction, trade.price);
            
            log.info("Quant Agent 识别到大单: code={}, amount={}万, threshold={}万, intent={}", 
                    stockCode, trade.amount / 1000000.0, threshold / 1000000.0, intent);
            
            // 发布系统事件
            LargeOrderEvent event = new LargeOrderEvent(
                    this,
                    trade.stockCode,
                    trade.amount,
                    trade.price,
                    trade.direction
            );
            eventPublisher.publishEvent(event);
        } else {
            log.debug("普通成交: amount={}, threshold={}", trade.amount, threshold);
        }
    }

    @Scheduled(fixedRate = 3000) // 每 3 秒执行一次
    public void scheduleMockTask() {
        log.info("Scheduled mock task execution...");
        // 模拟处理一个股票代码，例如 "SH600000"
        processRealTimeStream("SH600000");
    }
    
    /**
     * 计算动态阈值
     */
    private long calculateDynamicThreshold(String stockCode) {
        // 尝试从缓存获取阈值
        String cacheKey = THRESHOLD_CACHE_PREFIX + stockCode;
        try {
            String cachedThreshold = redisTemplate.opsForValue().get(cacheKey);
            if (cachedThreshold != null) {
                return Long.parseLong(cachedThreshold);
            }
        } catch (Exception e) {
            log.warn("Redis read failed for threshold: {}", e.getMessage());
        }
        
        // 如果没有足够的近期数据，使用默认阈值
        if (recentTradeAmounts.size() < 50) {
            long defaultThreshold = 50_000_000L; // 默认50万
            // 缓存默认阈值
            cacheThreshold(stockCode, defaultThreshold);
            return defaultThreshold;
        }
        
        // 计算均值和标准差 - 使用并行计算提高性能
        int count = recentTradeCount.get();
        long[] data = Arrays.copyOf(recentTradeAmounts, count);
        
        // 并行计算均值
        double mean = Arrays.stream(data).parallel().mapToDouble(Long::doubleValue).average().orElse(0);
        
        // 并行计算方差
        double variance = Arrays.stream(data).parallel()
                .mapToDouble(amount -> Math.pow(amount - mean, 2))
                .average().orElse(0);
        double standardDeviation = Math.sqrt(variance);
        
        // 动态阈值 = 均值 + N倍标准差
        long dynamicThreshold = (long)(mean + STANDARD_DEVIATION_MULTIPLE * standardDeviation);
        
        // 确保阈值不小于最小阈值 (20万)
        dynamicThreshold = Math.max(dynamicThreshold, 20_000_000L);
        
        // 缓存动态阈值
        cacheThreshold(stockCode, dynamicThreshold);
        
        return dynamicThreshold;
    }
    
    /**
     * 更新近期交易数据 - 使用环形数组提高性能
     */
    private void updateRecentTrades(long amount) {
        int index = recentTradeIndex.getAndIncrement();
        if (index >= RECENT_TRADES_SIZE) {
            // 重置索引，形成环形数组
            recentTradeIndex.set(1);
            index = 0;
        }
        recentTradeAmounts[index] = amount;
        if (recentTradeCount.get() < RECENT_TRADES_SIZE) {
            recentTradeCount.incrementAndGet();
        }
    }
    
    /**
     * 缓存阈值
     */
    private void cacheThreshold(String stockCode, long threshold) {
        try {
            String cacheKey = THRESHOLD_CACHE_PREFIX + stockCode;
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(threshold), THRESHOLD_CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis write failed for threshold: {}", e.getMessage());
        }
    }
    
    /**
     * 识别大单意图
     */
    private String identifyIntent(long amount, long threshold, String direction, int price) {
        String intent;
        
        // 根据交易金额超出阈值的比例和方向判断意图
        double exceedRatio = (double)amount / threshold;
        
        if ("BUY".equals(direction)) {
            if (exceedRatio > 2.0) {
                intent = "吸筹";
            } else if (exceedRatio > 1.5) {
                intent = "主动买入";
            } else {
                intent = "买入";
            }
        } else if ("SELL".equals(direction)) {
            if (exceedRatio > 2.0) {
                intent = "出货";
            } else if (exceedRatio > 1.5) {
                intent = "主动卖出";
            } else {
                intent = "卖出";
            }
        } else {
            intent = "中性";
        }
        
        return intent;
    }

    // ----------------- Mock 辅助方法 -----------------

    private TradeData mockLevel2Data(String stockCode) {
        Random random = new Random();
        
        // 随机生成成交金额: 1万 - 1亿 (分)
        long amount = 10_000L + random.nextInt(100_000_000);
        
        // 随机价格: 9.00 - 11.00 元 (900 - 1100 分)
        int price = 900 + random.nextInt(200);
        
        // 随机方向
        String direction = random.nextBoolean() ? "BUY" : "SELL";

        return new TradeData(stockCode, price, amount, direction);
    }

    private record TradeData(String stockCode, int price, long amount, String direction) {}

    /**
     * 获取指定股票的近期大单净流入金额 (Mock)
     * @param stockCode 股票代码
     * @param days 天数
     * @return 净流入金额 (分)
     */
    public long getNetInflow(String stockCode, int days) {
        String cacheKey = CACHE_KEY_PREFIX + stockCode + ":" + days;
        
        // 1. 查缓存
        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                return Long.parseLong(cachedValue);
            }
        } catch (Exception e) {
            log.warn("Redis read failed: {}", e.getMessage());
        }

        // 2. 模拟数据: 随机返回正负值
        // 实际应查询数据库统计 sum(buy_amount) - sum(sell_amount)
        Random random = new Random();
        boolean isInflow = random.nextBoolean(); 
        long amount = random.nextInt(100_000_000); // 0 - 1亿
        long result = isInflow ? amount : -amount;

        // 3. 写缓存 (5秒过期)
        try {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(result), 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis write failed: {}", e.getMessage());
        }
        
        return result;
    }
}
