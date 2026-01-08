package com.stock.analysis.module.tech;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

/**
 * 技术指标计算器 (MA, MACD, RSI, BOLL)
 * 支持手动实现和TA-Lib库实现
 */
public class IndicatorCalculator {
    
    // TA-Lib核心实例，线程安全
    private static final Core TA_LIB_CORE = new Core();
    // 用于TA-Lib计算的临时索引对象
    private static final ThreadLocal<MInteger> OUT_BEG = ThreadLocal.withInitial(MInteger::new);
    private static final ThreadLocal<MInteger> OUT_NB_ELEMENTS = ThreadLocal.withInitial(MInteger::new);

    /**
     * 计算移动平均线 (MA) - 默认使用TA-Lib实现
     */
    public static List<Integer> calculateMA(List<Kline> klines, int period) {
        return calculateMA(klines, period, true);
    }

    /**
     * 计算移动平均线 (MA)
     */
    public static List<Integer> calculateMA(List<Kline> klines, int period, boolean useTALib) {
        if (klines == null || klines.size() < period) {
            return new ArrayList<>();
        }

        if (useTALib) {
            return calculateMAWithTALib(klines, period);
        } else {
            return calculateMAWithManual(klines, period);
        }
    }

    /**
     * 计算移动平均线 (MA) - TA-Lib实现
     */
    private static List<Integer> calculateMAWithTALib(List<Kline> klines, int period) {
        int size = klines.size();
        List<Integer> maList = new ArrayList<>(size);
        double[] close = new double[size];
        double[] output = new double[size];

        for (int i = 0; i < size; i++) {
            close[i] = klines.get(i).getClose();
            maList.add(0); // 初始化
        }

        MInteger outBegIdx = OUT_BEG.get();
        MInteger outNbElement = OUT_NB_ELEMENTS.get();

        RetCode retCode = TA_LIB_CORE.sma(0, size - 1, close, period, outBegIdx, outNbElement, output);
        if (retCode == RetCode.Success) {
            for (int i = outBegIdx.value; i < size; i++) {
                maList.set(i, (int) Math.round(output[i - outBegIdx.value]));
            }
        }

        return maList;
    }

    /**
     * 计算移动平均线 (MA) - 手动实现
     */
    private static List<Integer> calculateMAWithManual(List<Kline> klines, int period) {
        List<Integer> maList = new ArrayList<>(klines.size());
        for (int i = 0; i < klines.size(); i++) {
            if (i < period - 1) {
                maList.add(0);
            } else {
                double sum = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += klines.get(j).getClose();
                }
                maList.add((int) Math.round(sum / period));
            }
        }
        return maList;
    }

    /**
     * 计算 RSI (相对强弱指标) - 默认使用TA-Lib实现
     */
    public static List<Double> calculateRSI(List<Kline> klines, int period) {
        return calculateRSI(klines, period, true);
    }

    /**
     * 计算 RSI (相对强弱指标)
     */
    public static List<Double> calculateRSI(List<Kline> klines, int period, boolean useTALib) {
        if (klines == null || klines.size() <= period) {
            return new ArrayList<>();
        }

        if (useTALib) {
            return calculateRSIWithTALib(klines, period);
        } else {
            return calculateRSIWithManual(klines, period);
        }
    }

    /**
     * 计算 RSI (相对强弱指标) - TA-Lib实现
     */
    private static List<Double> calculateRSIWithTALib(List<Kline> klines, int period) {
        int size = klines.size();
        List<Double> rsiList = new ArrayList<>(size);
        double[] close = new double[size];
        double[] output = new double[size];

        for (int i = 0; i < size; i++) {
            close[i] = klines.get(i).getClose();
            rsiList.add(0.0); // 初始化
        }

        MInteger outBegIdx = OUT_BEG.get();
        MInteger outNbElement = OUT_NB_ELEMENTS.get();

        RetCode retCode = TA_LIB_CORE.rsi(0, size - 1, close, period, outBegIdx, outNbElement, output);
        if (retCode == RetCode.Success) {
            for (int i = outBegIdx.value; i < size; i++) {
                rsiList.set(i, output[i - outBegIdx.value]);
            }
        }

        return rsiList;
    }

    /**
     * 计算 RSI (相对强弱指标) - 手动实现
     */
    private static List<Double> calculateRSIWithManual(List<Kline> klines, int period) {
        List<Double> rsiList = new ArrayList<>(klines.size());
        List<Double> gains = new ArrayList<>(klines.size());
        List<Double> losses = new ArrayList<>(klines.size());

        // 初始化前period个值
        for (int i = 0; i < klines.size(); i++) {
            if (i == 0) {
                gains.add(0.0);
                losses.add(0.0);
            } else {
                double change = klines.get(i).getClose() - klines.get(i - 1).getClose();
                if (change > 0) {
                    gains.add(change);
                    losses.add(0.0);
                } else {
                    gains.add(0.0);
                    losses.add(Math.abs(change));
                }
            }
        }

        // 计算RSI
        double avgGain = 0, avgLoss = 0;
        for (int i = 0; i < period; i++) {
            avgGain += gains.get(i);
            avgLoss += losses.get(i);
        }
        avgGain /= period;
        avgLoss /= period;

        for (int i = 0; i < klines.size(); i++) {
            if (i < period - 1) {
                rsiList.add(0.0);
            } else if (i == period - 1) {
                double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
                double rsi = 100 - (100 / (1 + rs));
                rsiList.add(rsi);
            } else {
                avgGain = (avgGain * (period - 1) + gains.get(i)) / period;
                avgLoss = (avgLoss * (period - 1) + losses.get(i)) / period;
                double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
                double rsi = 100 - (100 / (1 + rs));
                rsiList.add(rsi);
            }
        }
        return rsiList;
    }

    /**
     * 计算MACD指标 - 默认使用TA-Lib实现
     */
    public static MACDResult calculateMACD(List<Kline> klines, int fastPeriod, int slowPeriod, int signalPeriod) {
        return calculateMACD(klines, fastPeriod, slowPeriod, signalPeriod, true);
    }

    /**
     * 计算MACD指标
     */
    public static MACDResult calculateMACD(List<Kline> klines, int fastPeriod, int slowPeriod, int signalPeriod, boolean useTALib) {
        if (klines == null || klines.size() < slowPeriod) {
            return new MACDResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        if (useTALib) {
            return calculateMACDWithTALib(klines, fastPeriod, slowPeriod, signalPeriod);
        } else {
            return calculateMACDWithManual(klines, fastPeriod, slowPeriod, signalPeriod);
        }
    }

    /**
     * 计算MACD指标 - TA-Lib实现
     */
    private static MACDResult calculateMACDWithTALib(List<Kline> klines, int fastPeriod, int slowPeriod, int signalPeriod) {
        int size = klines.size();
        List<Double> macdList = new ArrayList<>(size);
        List<Double> signalList = new ArrayList<>(size);
        List<Double> histogramList = new ArrayList<>(size);
        double[] close = new double[size];
        double[] outputMACD = new double[size];
        double[] outputSignal = new double[size];
        double[] outputHistogram = new double[size];

        for (int i = 0; i < size; i++) {
            close[i] = klines.get(i).getClose();
            macdList.add(0.0);
            signalList.add(0.0);
            histogramList.add(0.0);
        }

        MInteger outBegIdx = OUT_BEG.get();
        MInteger outNbElement = OUT_NB_ELEMENTS.get();

        RetCode retCode = TA_LIB_CORE.macd(0, size - 1, close, fastPeriod, slowPeriod, signalPeriod, 
            outBegIdx, outNbElement, outputMACD, outputSignal, outputHistogram);
        
        if (retCode == RetCode.Success) {
            for (int i = outBegIdx.value; i < size; i++) {
                int outputIndex = i - outBegIdx.value;
                macdList.set(i, outputMACD[outputIndex]);
                signalList.set(i, outputSignal[outputIndex]);
                histogramList.set(i, outputHistogram[outputIndex]);
            }
        }

        return new MACDResult(macdList, signalList, histogramList);
    }

    /**
     * 计算MACD指标 - 手动实现
     */
    private static MACDResult calculateMACDWithManual(List<Kline> klines, int fastPeriod, int slowPeriod, int signalPeriod) {
        int size = klines.size();
        List<Double> macdList = new ArrayList<>(size);
        List<Double> signalList = new ArrayList<>(size);
        List<Double> histogramList = new ArrayList<>(size);

        // 计算快速和慢速EMA
        List<Double> fastEMA = calculateEMAValues(klines, fastPeriod);
        List<Double> slowEMA = calculateEMAValues(klines, slowPeriod);

        // 计算MACD线
        for (int i = 0; i < size; i++) {
            if (i < slowPeriod - 1) {
                macdList.add(0.0);
            } else {
                double macd = fastEMA.get(i) - slowEMA.get(i);
                macdList.add(macd);
            }
        }

        // 计算信号线
        List<Double> signalEMA = calculateEMAFromValues(macdList, signalPeriod);
        for (int i = 0; i < size; i++) {
            if (i < slowPeriod - 1 + signalPeriod - 1) {
                signalList.add(0.0);
            } else {
                signalList.add(signalEMA.get(i));
            }
        }

        // 计算柱状图
        for (int i = 0; i < size; i++) {
            double histogram = macdList.get(i) - (i < signalList.size() ? signalList.get(i) : 0.0);
            histogramList.add(histogram);
        }

        return new MACDResult(macdList, signalList, histogramList);
    }

    /**
     * 计算布林带指标 (BOLL) - 默认使用TA-Lib实现
     */
    public static BOLLResult calculateBOLL(List<Kline> klines, int period, int standardDeviations) {
        return calculateBOLL(klines, period, standardDeviations, true);
    }

    /**
     * 计算布林带指标 (BOLL)
     */
    public static BOLLResult calculateBOLL(List<Kline> klines, int period, int standardDeviations, boolean useTALib) {
        if (klines == null || klines.size() < period) {
            return new BOLLResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        if (useTALib) {
            return calculateBOLLWithTALib(klines, period, standardDeviations);
        } else {
            return calculateBOLLWithManual(klines, period, standardDeviations);
        }
    }

    /**
     * 计算布林带指标 (BOLL) - TA-Lib实现
     */
    private static BOLLResult calculateBOLLWithTALib(List<Kline> klines, int period, int standardDeviations) {
        int size = klines.size();
        List<Integer> upperList = new ArrayList<>(size);
        List<Integer> middleList = new ArrayList<>(size);
        List<Integer> lowerList = new ArrayList<>(size);
        double[] close = new double[size];
        double[] outputUpper = new double[size];
        double[] outputMiddle = new double[size];
        double[] outputLower = new double[size];

        for (int i = 0; i < size; i++) {
            close[i] = klines.get(i).getClose();
            upperList.add(0);
            middleList.add(0);
            lowerList.add(0);
        }

        MInteger outBegIdx = OUT_BEG.get();
        MInteger outNbElement = OUT_NB_ELEMENTS.get();

        RetCode retCode = TA_LIB_CORE.bbands(0, size - 1, close, period, standardDeviations, 
            standardDeviations, MAType.Sma, 
            outBegIdx, outNbElement, outputUpper, outputMiddle, outputLower);
        
        if (retCode == RetCode.Success) {
            for (int i = outBegIdx.value; i < size; i++) {
                int outputIndex = i - outBegIdx.value;
                upperList.set(i, (int) Math.round(outputUpper[outputIndex]));
                middleList.set(i, (int) Math.round(outputMiddle[outputIndex]));
                lowerList.set(i, (int) Math.round(outputLower[outputIndex]));
            }
        }

        return new BOLLResult(upperList, middleList, lowerList);
    }

    /**
     * 计算布林带指标 (BOLL) - 手动实现
     */
    private static BOLLResult calculateBOLLWithManual(List<Kline> klines, int period, int standardDeviations) {
        int size = klines.size();
        List<Integer> upperList = new ArrayList<>(size);
        List<Integer> middleList = new ArrayList<>(size);
        List<Integer> lowerList = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                upperList.add(0);
                middleList.add(0);
                lowerList.add(0);
            } else {
                // 计算中轨（MA）
                double middle = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    middle += klines.get(j).getClose();
                }
                middle /= period;

                // 计算标准差
                double sumSquares = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    double diff = klines.get(j).getClose() - middle;
                    sumSquares += diff * diff;
                }
                double stdDev = Math.sqrt(sumSquares / period);

                // 计算上下轨
                double upper = middle + (standardDeviations * stdDev);
                double lower = middle - (standardDeviations * stdDev);

                middleList.add((int) Math.round(middle));
                upperList.add((int) Math.round(upper));
                lowerList.add((int) Math.round(lower));
            }
        }

        return new BOLLResult(upperList, middleList, lowerList);
    }

    /**
     * 计算EMA值 - 辅助方法
     */
    private static List<Double> calculateEMAValues(List<Kline> klines, int period) {
        List<Double> emaValues = new ArrayList<>(klines.size());
        double multiplier = 2.0 / (period + 1);

        for (int i = 0; i < klines.size(); i++) {
            if (i < period - 1) {
                emaValues.add(0.0);
            } else if (i == period - 1) {
                // 第一个EMA值使用SMA
                double sma = 0;
                for (int j = 0; j < period; j++) {
                    sma += klines.get(j).getClose();
                }
                sma /= period;
                emaValues.add(sma);
            } else {
                double prevEMA = emaValues.get(i - 1);
                double currentClose = klines.get(i).getClose();
                double ema = (currentClose - prevEMA) * multiplier + prevEMA;
                emaValues.add(ema);
            }
        }
        return emaValues;
    }

    /**
     * 从值列表计算EMA - 辅助方法
     */
    private static List<Double> calculateEMAFromValues(List<Double> values, int period) {
        List<Double> emaValues = new ArrayList<>(values.size());
        double multiplier = 2.0 / (period + 1);

        for (int i = 0; i < values.size(); i++) {
            if (i < period - 1) {
                emaValues.add(0.0);
            } else if (i == period - 1) {
                // 第一个EMA值使用SMA
                double sma = 0;
                for (int j = 0; j < period; j++) {
                    sma += values.get(j);
                }
                sma /= period;
                emaValues.add(sma);
            } else {
                double prevEMA = emaValues.get(i - 1);
                double currentValue = values.get(i);
                double ema = (currentValue - prevEMA) * multiplier + prevEMA;
                emaValues.add(ema);
            }
        }
        return emaValues;
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
     * 计算指数移动平均线 (EMA) - 手动实现
     */
    public static List<Integer> calculateEMA(List<Kline> klines, int period) {
        if (klines == null || klines.size() < period) {
            return new ArrayList<>();
        }

        List<Double> emaValues = calculateEMAValues(klines, period);
        List<Integer> emaList = new ArrayList<>(klines.size());
        
        for (double ema : emaValues) {
            emaList.add((int) Math.round(ema));
        }
        
        return emaList;
    }
    
    /**
     * 计算成交量加权平均价 (VWAP) - 手动实现
     */
    public static List<Double> calculateVWAP(List<Kline> klines) {
        if (klines == null || klines.isEmpty()) {
            return new ArrayList<>();
        }

        List<Double> vwapList = new ArrayList<>(klines.size());
        long totalVolume = 0;
        double totalValue = 0.0;
        
        for (Kline kline : klines) {
            double typicalPrice = (kline.getHigh() + kline.getLow() + kline.getClose()) / 3.0;
            double value = typicalPrice * kline.getVolume();
            
            totalVolume += kline.getVolume();
            totalValue += value;
            
            double vwap = totalVolume > 0 ? totalValue / totalVolume : 0.0;
            vwapList.add(vwap);
        }
        
        return vwapList;
    }
    
    /**
     * 计算平均真实波幅 (ATR) - 手动实现
     */
    public static List<Double> calculateATR(List<Kline> klines, int period) {
        if (klines == null || klines.size() < period) {
            return new ArrayList<>();
        }

        int size = klines.size();
        List<Double> atrList = new ArrayList<>(size);
        List<Double> trList = new ArrayList<>(size);

        // 计算真实波幅 (TR)
        for (int i = 0; i < size; i++) {
            Kline kline = klines.get(i);
            double tr;

            if (i == 0) {
                // 第一根K线的TR就是最高价减最低价
                tr = kline.getHigh() - kline.getLow();
            } else {
                Kline prevKline = klines.get(i - 1);
                double highLowDiff = kline.getHigh() - kline.getLow();
                double highPrevCloseDiff = Math.abs(kline.getHigh() - prevKline.getClose());
                double lowPrevCloseDiff = Math.abs(kline.getLow() - prevKline.getClose());
                tr = Math.max(Math.max(highLowDiff, highPrevCloseDiff), lowPrevCloseDiff);
            }

            trList.add(tr);
        }

        // 计算ATR (使用EMA平滑)
        List<Double> smoothedTR = new ArrayList<>(size);
        double multiplier = 1.0 / period;

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                smoothedTR.add(0.0);
            } else if (i == period - 1) {
                // 第一个ATR值使用简单平均
                double sumTR = 0.0;
                for (int j = 0; j < period; j++) {
                    sumTR += trList.get(i - period + 1 + j);
                }
                smoothedTR.add(sumTR / period);
            } else {
                // 后续使用EMA计算
                double prevATR = smoothedTR.get(i - 1);
                double currentTR = trList.get(i);
                double atr = prevATR + multiplier * (currentTR - prevATR);
                smoothedTR.add(atr);
            }
        }

        return smoothedTR;
    }
    
    /**
     * 计算威廉指标 (WR) - 手动实现
     */
    public static List<Double> calculateWR(List<Kline> klines, int period) {
        if (klines == null || klines.size() < period) {
            return new ArrayList<>();
        }

        List<Double> wrList = new ArrayList<>(klines.size());
        
        for (int i = 0; i < klines.size(); i++) {
            if (i < period - 1) {
                wrList.add(0.0);
            } else {
                // 计算period周期内的最高价和最低价
                double highest = Double.MIN_VALUE;
                double lowest = Double.MAX_VALUE;
                
                for (int j = i - period + 1; j <= i; j++) {
                    Kline kline = klines.get(j);
                    highest = Math.max(highest, kline.getHigh());
                    lowest = Math.min(lowest, kline.getLow());
                }
                
                double close = klines.get(i).getClose();
                
                if (highest != lowest) {
                    double wr = -100 * (highest - close) / (highest - lowest);
                    wrList.add(wr);
                } else {
                    wrList.add(0.0);
                }
            }
        }
        
        return wrList;
    }
    
    /**
     * 计算能量潮指标 (OBV) - 手动实现
     */
    public static List<Long> calculateOBV(List<Kline> klines) {
        if (klines == null || klines.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> obvList = new ArrayList<>(klines.size());
        long obv = 0;
        
        for (int i = 0; i < klines.size(); i++) {
            if (i == 0) {
                obv = klines.get(i).getVolume();
            } else {
                double currentClose = klines.get(i).getClose();
                double previousClose = klines.get(i - 1).getClose();
                
                if (currentClose > previousClose) {
                    obv += klines.get(i).getVolume();
                } else if (currentClose < previousClose) {
                    obv -= klines.get(i).getVolume();
                }
            }
            
            obvList.add(obv);
        }
        
        return obvList;
    }
    
    /**
     * 计算KDJ指标 - 手动实现
     */
    public static KDJResult calculateKDJ(List<Kline> klines, int n, int m1, int m2) {
        if (klines == null || klines.size() < n + m1 + m2) {
            return new KDJResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        int size = klines.size();
        List<Double> kList = new ArrayList<>(size);
        List<Double> dList = new ArrayList<>(size);
        List<Double> jList = new ArrayList<>(size);
        
        // 计算K值的基础数据
        List<Double> rsvList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            if (i < n - 1) {
                rsvList.add(0.0);
            } else {
                // 计算n周期内的最高价和最低价
                double highest = Double.MIN_VALUE;
                double lowest = Double.MAX_VALUE;
                
                for (int j = i - n + 1; j <= i; j++) {
                    Kline kline = klines.get(j);
                    highest = Math.max(highest, kline.getHigh());
                    lowest = Math.min(lowest, kline.getLow());
                }
                
                double close = klines.get(i).getClose();
                
                if (highest != lowest) {
                    rsvList.add(100 * (close - lowest) / (highest - lowest));
                } else {
                    rsvList.add(50.0);
                }
            }
        }
        
        // 计算KDJ
        double prevK = 50.0;
        double prevD = 50.0;
        
        for (int i = 0; i < size; i++) {
            if (i < n - 1) {
                kList.add(0.0);
                dList.add(0.0);
                jList.add(0.0);
            } else if (i == n - 1) {
                double k = rsvList.get(i);
                double d = k;
                double j = 3 * k - 2 * d;
                
                kList.add(k);
                dList.add(d);
                jList.add(j);
                
                prevK = k;
                prevD = d;
            } else {
                double k = (prevK * (m1 - 1) + rsvList.get(i)) / m1;
                double d = (prevD * (m2 - 1) + k) / m2;
                double j = 3 * k - 2 * d;
                
                kList.add(k);
                dList.add(d);
                jList.add(j);
                
                prevK = k;
                prevD = d;
            }
        }
        
        return new KDJResult(kList, dList, jList);
    }
    
    /**
     * 计算SAR指标 - 手动实现
     */
    public static SARResult calculateSAR(List<Kline> klines, double acceleration, double maxAcceleration) {
        if (klines == null || klines.isEmpty()) {
            return new SARResult(new ArrayList<>(), new ArrayList<>());
        }

        List<Double> sarList = new ArrayList<>(klines.size());
        List<String> signalList = new ArrayList<>(klines.size());
        
        // 初始设置
        boolean isLong = true;
        double sar = klines.get(0).getLow();
        double ep = klines.get(0).getHigh();
        double af = acceleration;
        
        for (int i = 0; i < klines.size(); i++) {
            Kline kline = klines.get(i);
            
            if (i == 0) {
                sarList.add(sar);
                signalList.add("NEUTRAL");
                continue;
            }
            
            // 更新SAR
            if (isLong) {
                sar = sar + af * (ep - sar);
                
                // 限制SAR不超过前一根K线的最低价
                sar = Math.min(sar, klines.get(i - 1).getLow());
                
                // 检查反转
                if (kline.getLow() < sar) {
                    isLong = false;
                    sar = ep;
                    af = acceleration;
                    ep = kline.getLow();
                } else {
                    // 更新极值和加速因子
                    if (kline.getHigh() > ep) {
                        ep = kline.getHigh();
                        af = Math.min(af + acceleration, maxAcceleration);
                    }
                }
            } else {
                sar = sar - af * (sar - ep);
                
                // 限制SAR不低于前一根K线的最高价
                sar = Math.max(sar, klines.get(i - 1).getHigh());
                
                // 检查反转
                if (kline.getHigh() > sar) {
                    isLong = true;
                    sar = ep;
                    af = acceleration;
                    ep = kline.getHigh();
                } else {
                    // 更新极值和加速因子
                    if (kline.getLow() < ep) {
                        ep = kline.getLow();
                        af = Math.min(af + acceleration, maxAcceleration);
                    }
                }
            }
            
            sarList.add(sar);
            
            // 生成交易信号
            double prevSar = sarList.get(i - 1);
            double prevClose = klines.get(i - 1).getClose();
            double currentClose = kline.getClose();
            
            if (prevClose > prevSar && currentClose < sar) {
                signalList.add("SELL");
            } else if (prevClose < prevSar && currentClose > sar) {
                signalList.add("BUY");
            } else {
                signalList.add("NEUTRAL");
            }
        }
        
        return new SARResult(sarList, signalList);
    }
    
    /**
     * 计算DMI指标 - 手动实现
     */
    public static DMIResult calculateDMI(List<Kline> klines, int period) {
        if (klines == null || klines.size() < period) {
            return new DMIResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        int size = klines.size();
        List<Double> pdiList = new ArrayList<>(size);
        List<Double> mdiList = new ArrayList<>(size);
        List<Double> adxList = new ArrayList<>(size);
        
        // 计算TR、+DM、-DM
        List<Double> trList = new ArrayList<>(size);
        List<Double> pdmList = new ArrayList<>(size);
        List<Double> mdmList = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            if (i == 0) {
                trList.add((double)(klines.get(i).getHigh() - klines.get(i).getLow()));
                pdmList.add(0.0);
                mdmList.add(0.0);
            } else {
                Kline current = klines.get(i);
                Kline previous = klines.get(i - 1);
                
                // 计算TR
                double tr1 = current.getHigh() - current.getLow();
                double tr2 = Math.abs(current.getHigh() - previous.getClose());
                double tr3 = Math.abs(current.getLow() - previous.getClose());
                trList.add(Math.max(Math.max(tr1, tr2), tr3));
                
                // 计算+DM和-DM
                double upMove = current.getHigh() - previous.getHigh();
                double downMove = previous.getLow() - current.getLow();
                
                if (upMove > downMove && upMove > 0) {
                    pdmList.add(upMove);
                    mdmList.add(0.0);
                } else if (downMove > upMove && downMove > 0) {
                    pdmList.add(0.0);
                    mdmList.add(downMove);
                } else {
                    pdmList.add(0.0);
                    mdmList.add(0.0);
                }
            }
        }
        
        // 计算平滑的+DM、-DM和TR
        List<Double> smoothedPDM = new ArrayList<>(size);
        List<Double> smoothedMDM = new ArrayList<>(size);
        List<Double> smoothedTR = new ArrayList<>(size);
        
        // 初始化第一个值
        double sumTR = 0.0, sumPDM = 0.0, sumMDM = 0.0;
        for (int i = 0; i < period; i++) {
            sumTR += trList.get(i);
            sumPDM += pdmList.get(i);
            sumMDM += mdmList.get(i);
        }
        
        smoothedTR.add(sumTR);
        smoothedPDM.add(sumPDM);
        smoothedMDM.add(sumMDM);
        
        // 计算后续的平滑值
        for (int i = period; i < size; i++) {
            double prevTR = smoothedTR.get(i - period);
            double prevPDM = smoothedPDM.get(i - period);
            double prevMDM = smoothedMDM.get(i - period);
            
            double currentTR = prevTR - prevTR / period + trList.get(i);
            double currentPDM = prevPDM - prevPDM / period + pdmList.get(i);
            double currentMDM = prevMDM - prevMDM / period + mdmList.get(i);
            
            smoothedTR.add(currentTR);
            smoothedPDM.add(currentPDM);
            smoothedMDM.add(currentMDM);
        }
        
        // 计算PDI和MDI
        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                pdiList.add(0.0);
                mdiList.add(0.0);
            } else {
                int idx = i - (period - 1);
                double tr = smoothedTR.get(idx);
                if (tr > 0) {
                    pdiList.add(100 * smoothedPDM.get(idx) / tr);
                    mdiList.add(100 * smoothedMDM.get(idx) / tr);
                } else {
                    pdiList.add(0.0);
                    mdiList.add(0.0);
                }
            }
        }
        
        // 计算DX和ADX
        List<Double> dxList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                dxList.add(0.0);
            } else {
                double pdi = pdiList.get(i);
                double mdi = mdiList.get(i);
                double sum = pdi + mdi;
                if (sum > 0) {
                    dxList.add(100 * Math.abs(pdi - mdi) / sum);
                } else {
                    dxList.add(0.0);
                }
            }
        }
        
        // 计算ADX
        for (int i = 0; i < size; i++) {
            if (i < 2 * period - 2) {
                adxList.add(0.0);
            } else {
                // 计算ADX
                double sumDX = 0.0;
                for (int j = i - period + 1; j <= i; j++) {
                    sumDX += dxList.get(j);
                }
                adxList.add(sumDX / period);
            }
        }
        
        return new DMIResult(pdiList, mdiList, adxList);
    }
    
    /**
     * KDJ指标结果类
     */
    public static class KDJResult {
        private final List<Double> k;
        private final List<Double> d;
        private final List<Double> j;

        public KDJResult(List<Double> k, List<Double> d, List<Double> j) {
            this.k = k;
            this.d = d;
            this.j = j;
        }

        public List<Double> getK() { return k; }
        public List<Double> getD() { return d; }
        public List<Double> getJ() { return j; }
    }
    
    /**
     * SAR指标结果类
     */
    public static class SARResult {
        private final List<Double> sar;
        private final List<String> signals;

        public SARResult(List<Double> sar, List<String> signals) {
            this.sar = sar;
            this.signals = signals;
        }

        public List<Double> getSar() { return sar; }
        public List<String> getSignals() { return signals; }
    }
    
    /**
     * DMI指标结果类
     */
    public static class DMIResult {
        private final List<Double> pdi;
        private final List<Double> mdi;
        private final List<Double> adx;

        public DMIResult(List<Double> pdi, List<Double> mdi, List<Double> adx) {
            this.pdi = pdi;
            this.mdi = mdi;
            this.adx = adx;
        }

        public List<Double> getPdi() { return pdi; }
        public List<Double> getMdi() { return mdi; }
        public List<Double> getAdx() { return adx; }
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
