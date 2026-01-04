package com.stock.analysis.module.tech;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 技术指标计算器 (MA, MACD, RSI)
 */
public class IndicatorCalculator {

    /**
     * 计算移动平均线 (MA)
     */
    public static List<Integer> calculateMA(List<Kline> klines, int period) {
        List<Integer> maList = new ArrayList<>();
        if (klines == null || klines.size() < period) {
            return maList;
        }

        long sum = 0;
        for (int i = 0; i < klines.size(); i++) {
            sum += klines.get(i).getClose();
            if (i >= period) {
                sum -= klines.get(i - period).getClose();
            }
            if (i >= period - 1) {
                maList.add((int) (sum / period));
            } else {
                maList.add(0); // 填充
            }
        }
        return maList;
    }

    /**
     * 计算 RSI (相对强弱指标)
     */
    public static List<Double> calculateRSI(List<Kline> klines, int period) {
        List<Double> rsiList = new ArrayList<>();
        if (klines == null || klines.size() <= period) {
            return rsiList;
        }

        double gainSum = 0;
        double lossSum = 0;

        // 初始化第一个 RSI
        for (int i = 1; i <= period; i++) {
            int change = klines.get(i).getClose() - klines.get(i - 1).getClose();
            if (change > 0) gainSum += change;
            else lossSum -= change;
        }

        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        rsiList.add(100.0 - (100.0 / (1.0 + avgGain / (avgLoss == 0 ? 1 : avgLoss))));

        // 平滑计算后续 RSI
        for (int i = period + 1; i < klines.size(); i++) {
            int change = klines.get(i).getClose() - klines.get(i - 1).getClose();
            double currentGain = change > 0 ? change : 0;
            double currentLoss = change < 0 ? -change : 0;

            avgGain = (avgGain * (period - 1) + currentGain) / period;
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period;

            double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
            rsiList.add(100.0 - (100.0 / (1.0 + rs)));
        }
        
        // 补齐前面的空缺
        List<Double> result = new ArrayList<>(Collections.nCopies(period, 0.0));
        result.addAll(rsiList);
        return result;
    }
}
