package com.stock.analysis.module.risk;

import java.time.LocalDate;
import java.util.List;

/**
 * 回测引擎接口
 */
public interface BacktestEngine {

    /**
     * 扫描过去指定时间内满足评分条件的信号
     * @param stockCode 股票代码
     * @param years 过去几年
     * @param minScore 最低评分阈值
     * @return 满足条件的信号列表
     */
    List<BacktestSignal> scanHistoricalSignals(String stockCode, int years, double minScore);

    /**
     * 执行回测，计算胜率和盈亏比
     * @param stockCode 股票代码
     * @param signals 信号列表
     * @param holdingDaysList 持股天数列表
     * @return 回测结果
     */
    BacktestResult runBacktest(String stockCode, List<BacktestSignal> signals, List<Integer> holdingDaysList);

    /**
     * 获取历史同类信号的胜率
     * @param stockCode 股票代码
     * @param minScore 最低评分阈值
     * @param holdingDays 持股天数
     * @return 胜率（百分比）
     */
    double getHistoricalWinRate(String stockCode, double minScore, int holdingDays);

    /**
     * 自动复盘指定股票
     * @param stockCode 股票代码
     * @param years 过去几年
     * @param minScore 最低评分阈值
     * @return 回测结果
     */
    BacktestResult autoReview(String stockCode, int years, double minScore);
}
