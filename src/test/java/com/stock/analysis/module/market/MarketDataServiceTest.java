package com.stock.analysis.module.market;

import com.stock.analysis.module.tech.Kline;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行情数据服务单元测试
 */
@SpringBootTest
class MarketDataServiceTest {

    @Autowired
    private MarketDataService marketDataService;

    @Test
    void testGetRealTimeQuote() {
        // 测试获取实时行情
        RealTimeQuote quote = marketDataService.getRealTimeQuote("600519");
        assertNotNull(quote);
        assertEquals("600519", quote.getStockCode());
        assertNotNull(quote.getCurrentPrice());
        assertNotNull(quote.getUpdateTime());
        System.out.println("Real-time quote test passed: " + quote);
    }

    @Test
    void testGetHistoricalKlines() {
        // 测试获取历史K线数据
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        List<Kline> klines = marketDataService.getHistoricalKlines(
                "600519", startDate, endDate, MarketDataService.KlinePeriod.DAY);
        
        assertNotNull(klines);
        assertFalse(klines.isEmpty());
        assertEquals(31, klines.size()); // 30天 + 今天
        assertNotNull(klines.get(0).getDate());
        assertNotNull(klines.get(0).getClose());
        System.out.println("Historical klines test passed: " + klines.size() + " klines");
    }

    @Test
    void testGetBatchRealTimeQuotes() {
        // 测试批量获取实时行情
        List<String> stockCodes = List.of("600519", "000001", "000858");
        List<RealTimeQuote> quotes = marketDataService.getBatchRealTimeQuotes(stockCodes);
        
        assertNotNull(quotes);
        assertEquals(3, quotes.size());
        System.out.println("Batch real-time quotes test passed: " + quotes.size() + " quotes");
    }

    @Test
    void testGetBatchRealTimeQuotesWithEmptyList() {
        // 测试空列表情况
        List<RealTimeQuote> quotes = marketDataService.getBatchRealTimeQuotes(List.of());
        assertNotNull(quotes);
        assertTrue(quotes.isEmpty());
        System.out.println("Empty list batch quotes test passed");
    }
}