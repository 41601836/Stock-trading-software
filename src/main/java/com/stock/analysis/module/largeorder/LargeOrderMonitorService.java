package com.stock.analysis.module.largeorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Random;

import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LargeOrderMonitorService {

    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;
    private static final long LARGE_ORDER_THRESHOLD = 50_000_000L; // 50万 (单位: 分)
    private static final String CACHE_KEY_PREFIX = "largeorder:netinflow:";

    /**
     * 模拟接收 Level-2 实时成交流
     * 实际场景中，这里可能是 Kafka 消费者或 TCP Socket 回调
     */
    public void processRealTimeStream(String stockCode) {
        // 模拟生成一笔交易数据
        TradeData trade = mockLevel2Data(stockCode);

        // 监控逻辑: 判断是否为大单
        if (trade.amount > LARGE_ORDER_THRESHOLD) {
            log.info("Quant Agent 识别到大单: code={}, amount={}万", stockCode, trade.amount / 1000000.0);
            
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
            log.debug("普通成交: amount={}", trade.amount);
        }
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
