package com.stock.analysis.module.market;

import com.stock.analysis.module.tech.Kline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {

    @Override
    public RealTimeQuote getRealTimeQuote(String stockCode) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock code cannot be empty");
        }

        // 模拟实时行情数据 (实际应调用第三方行情接口)
        Random random = new Random();
        int prevClosePrice = 1000 + random.nextInt(500); // 昨收价 10-15元
        int currentPrice = prevClosePrice + (random.nextInt(200) - 100); // 当前价波动范围 -1到+1元
        int openPrice = prevClosePrice + (random.nextInt(100) - 50);
        int highPrice = Math.max(currentPrice, openPrice) + random.nextInt(100);
        int lowPrice = Math.min(currentPrice, openPrice) - random.nextInt(100);
        int changeAmount = currentPrice - prevClosePrice;
        double changePercent = (double) changeAmount / prevClosePrice * 100;
        long volume = 1000000L + random.nextLong(9000000L); // 成交量 100-1000万股
        long amount = (long) currentPrice * volume;

        return RealTimeQuote.builder()
                .stockCode(stockCode)
                .stockName(stockCode + "股票")
                .currentPrice(currentPrice)
                .openPrice(openPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .prevClosePrice(prevClosePrice)
                .volume(volume)
                .amount(amount)
                .changeAmount(changeAmount)
                .changePercent(Math.round(changePercent * 100.0) / 100.0) // 保留两位小数
                .updateTime(LocalDateTime.now())
                .build();
    }

    @Override
    public List<Kline> getHistoricalKlines(String stockCode, LocalDate startDate, LocalDate endDate, KlinePeriod period) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock code cannot be empty");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        // 计算日期范围
        long daysBetween = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        List<Kline> klines = new ArrayList<>();
        Random random = new Random();

        // 初始价格 10-15元
        int basePrice = 1000 + random.nextInt(500);
        int currentPrice = basePrice;

        for (long i = 0; i < daysBetween; i++) {
            LocalDate date = startDate.plusDays(i);
            int open = currentPrice + (random.nextInt(100) - 50);
            int close = open + (random.nextInt(200) - 100);
            int high = Math.max(open, close) + random.nextInt(100);
            int low = Math.min(open, close) - random.nextInt(100);
            long volume = 1000000L + random.nextLong(9000000L); // 成交量 100-1000万股
            long amount = (long) close * volume;

            Kline kline = Kline.builder()
                    .date(date)
                    .open(open)
                    .close(close)
                    .high(high)
                    .low(low)
                    .volume(volume)
                    .amount(amount)
                    .build();

            klines.add(kline);
            currentPrice = close;
        }

        log.info("Generated {} historical klines for stock {} from {} to {}", klines.size(), stockCode, startDate, endDate);
        return klines;
    }

    @Override
    public List<RealTimeQuote> getBatchRealTimeQuotes(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return new ArrayList<>();
        }

        // 去重
        List<String> uniqueCodes = stockCodes.stream().distinct().collect(Collectors.toList());

        // 使用并行流批量获取实时行情，提高处理效率
        return uniqueCodes.parallelStream()
                .map(stockCode -> {
                    try {
                        return getRealTimeQuote(stockCode);
                    } catch (Exception e) {
                        log.error("Failed to get real-time quote for stock {}", stockCode, e);
                        return null; // 返回null，后续过滤掉
                    }
                })
                .filter(quote -> quote != null) // 过滤掉获取失败的结果
                .collect(Collectors.toList());
    }
}