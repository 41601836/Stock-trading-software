package com.stock.analysis.module.market;

import com.stock.analysis.module.tech.Kline;

import java.time.LocalDate;
import java.util.List;

/**
 * 行情数据服务接口
 */
public interface MarketDataService {

    /**
     * 获取实时行情
     * @param stockCode 股票代码
     * @return 实时行情数据
     */
    RealTimeQuote getRealTimeQuote(String stockCode);

    /**
     * 获取历史K线数据
     * @param stockCode 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param period K线周期 (DAY, WEEK, MONTH)
     * @return K线数据列表
     */
    List<Kline> getHistoricalKlines(String stockCode, LocalDate startDate, LocalDate endDate, KlinePeriod period);

    /**
     * 获取多个股票的实时行情
     * @param stockCodes 股票代码列表
     * @return 实时行情数据列表
     */
    List<RealTimeQuote> getBatchRealTimeQuotes(List<String> stockCodes);

    /**
     * K线周期枚举
     */
    enum KlinePeriod {
        DAY,   // 日线
        WEEK,  // 周线
        MONTH  // 月线
    }
}