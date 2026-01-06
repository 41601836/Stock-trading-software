package com.stock.analysis.module.tech;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BarSeriesBuilder;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarBuilder;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.BollingerBandsUpperIndicator;
import org.ta4j.core.num.DoubleNum;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 技术指标计算器 (MA, MACD, RSI, BOLL)
 * 集成TA4J库增强指标计算能力
 */
public class IndicatorCalculator {

    /**
     * 计算移动平均线 (MA) - 优化版
     */
    public static List<Integer> calculateMA(List<Kline> klines, int period) {
        if (klines == null || klines.size() < period) {
            return new ArrayList<>();
        }

        int size = klines.size();
        List<Integer> maList = new ArrayList<>(size);
        long sum = 0;

        // 初始化前period个元素的和
        for (int i = 0; i < period; i++) {
            sum += klines.get(i).getClose();
            maList.add(0); // 填充前period-1个元素
        }
        maList.set(period - 1, (int) (sum / period));

        // 计算剩余元素
        for (int i = period; i < size; i++) {
            sum += klines.get(i).getClose() - klines.get(i - period).getClose();
            maList.add((int) (sum / period));
        }
        
        return maList;
    }

    /**
     * 计算 RSI (相对强弱指标) - 优化版
     */
    public static List<Double> calculateRSI(List<Kline> klines, int period) {
        if (klines == null || klines.size() <= period) {
            return new ArrayList<>();
        }

        // 使用TA4J计算RSI
        BarSeries series = convertToBarSeries(klines);
        if (series == null || series.getBarCount() < period) {
            return new ArrayList<>();
        }

        RSIIndicator rsiIndicator = new RSIIndicator(series, period);
        List<Double> rsiList = new ArrayList<>(klines.size());

        // 填充前period个元素
        for (int i = 0; i < period; i++) {
            rsiList.add(0.0);
        }

        // 计算后续RSI值
        for (int i = period; i < klines.size(); i++) {
            if (i < series.getBarCount()) {
                rsiList.add(rsiIndicator.getValue(i).doubleValue());
            } else {
                rsiList.add(0.0);
            }
        }
        
        return rsiList;
    }

    /**
     * 计算MACD指标 (使用TA4J)
     */
    public static MACDResult calculateMACD(List<Kline> klines, int fastPeriod, int slowPeriod, int signalPeriod) {
        BarSeries series = convertToBarSeries(klines);
        if (series == null || series.getBarCount() < slowPeriod) {
            return new MACDResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        MACDIndicator macdIndicator = new MACDIndicator(series, fastPeriod, slowPeriod);
        SMAIndicator signalIndicator = new SMAIndicator(macdIndicator, signalPeriod);

        List<Double> macdList = new ArrayList<>(klines.size());
        List<Double> signalList = new ArrayList<>(klines.size());
        List<Double> histogramList = new ArrayList<>(klines.size());

        // 填充前slowPeriod个元素
        for (int i = 0; i < slowPeriod; i++) {
            macdList.add(0.0);
            signalList.add(0.0);
            histogramList.add(0.0);
        }

        // 计算MACD值
        for (int i = slowPeriod; i < klines.size(); i++) {
            if (i < series.getBarCount()) {
                double macd = macdIndicator.getValue(i).doubleValue();
                double signal = signalIndicator.getValue(i).doubleValue();
                double histogram = macd - signal;

                macdList.add(macd);
                signalList.add(signal);
                histogramList.add(histogram);
            } else {
                macdList.add(0.0);
                signalList.add(0.0);
                histogramList.add(0.0);
            }
        }

        return new MACDResult(macdList, signalList, histogramList);
    }

    /**
     * 计算布林带指标 (使用TA4J)
     */
    public static BOLLResult calculateBOLL(List<Kline> klines, int period, int standardDeviations) {
        BarSeries series = convertToBarSeries(klines);
        if (series == null || series.getBarCount() < period) {
            return new BOLLResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        BollingerBandsMiddleIndicator middleIndicator = new BollingerBandsMiddleIndicator(new SMAIndicator(series, period));
        BollingerBandsUpperIndicator upperIndicator = new BollingerBandsUpperIndicator(middleIndicator, standardDeviations);
        BollingerBandsLowerIndicator lowerIndicator = new BollingerBandsLowerIndicator(middleIndicator, standardDeviations);

        List<Integer> upperList = new ArrayList<>(klines.size());
        List<Integer> middleList = new ArrayList<>(klines.size());
        List<Integer> lowerList = new ArrayList<>(klines.size());

        // 填充前period个元素
        for (int i = 0; i < period; i++) {
            upperList.add(0);
            middleList.add(0);
            lowerList.add(0);
        }

        // 计算布林带值
        for (int i = period; i < klines.size(); i++) {
            if (i < series.getBarCount()) {
                upperList.add((int) upperIndicator.getValue(i).doubleValue());
                middleList.add((int) middleIndicator.getValue(i).doubleValue());
                lowerList.add((int) lowerIndicator.getValue(i).doubleValue());
            } else {
                upperList.add(0);
                middleList.add(0);
                lowerList.add(0);
            }
        }

        return new BOLLResult(upperList, middleList, lowerList);
    }

    /**
     * 将Kline数据转换为TA4J的BarSeries
     */
    private static BarSeries convertToBarSeries(List<Kline> klines) {
        if (klines == null || klines.isEmpty()) {
            return null;
        }

        BarSeriesBuilder builder = new BarSeriesBuilder()
                .withName("Kline Series")
                .withNumTypeOf(DoubleNum.class);

        for (Kline kline : klines) {
            // 将LocalDate转换为LocalDateTime
            LocalDateTime dateTime = kline.getDate().atStartOfDay();
            long timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            BaseBar bar = new BaseBarBuilder()
                    .withTimePeriod(Duration.ofDays(1))
                    .withEndTime(dateTime)
                    .withOpenPrice(DoubleNum.valueOf(kline.getOpen()))
                    .withHighPrice(DoubleNum.valueOf(kline.getHigh()))
                    .withLowPrice(DoubleNum.valueOf(kline.getLow()))
                    .withClosePrice(DoubleNum.valueOf(kline.getClose()))
                    .withVolume(DoubleNum.valueOf(kline.getVolume()))
                    .withAmount(DoubleNum.valueOf(kline.getAmount()))
                    .build();

            builder.addBar(bar);
        }

        return builder.build();
    }

    /**
     * MACD指标结果类
     */
    public static class MACDResult {
        private final List<Double> macd;
        private final List<Double> signal;
        private final List<Double> histogram;

        public MACDResult(List<Double> macd, List<Double> signal, List<Double> histogram) {
            this.macd = macd;
            this.signal = signal;
            this.histogram = histogram;
        }

        public List<Double> getMacd() { return macd; }
        public List<Double> getSignal() { return signal; }
        public List<Double> getHistogram() { return histogram; }
    }

    /**
     * 布林带指标结果类
     */
    public static class BOLLResult {
        private final List<Integer> upper;
        private final List<Integer> middle;
        private final List<Integer> lower;

        public BOLLResult(List<Integer> upper, List<Integer> middle, List<Integer> lower) {
            this.upper = upper;
            this.middle = middle;
            this.lower = lower;
        }

        public List<Integer> getUpper() { return upper; }
        public List<Integer> getMiddle() { return middle; }
        public List<Integer> getLower() { return lower; }
    }
}
