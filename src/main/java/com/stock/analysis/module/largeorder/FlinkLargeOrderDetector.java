package com.stock.analysis.module.largeorder;

import com.stock.analysis.module.market.RealTimeQuote;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Flink 的大单异动检测器
 * 实现实时 Level-2 数据处理，动态阈值计算和大单意图识别
 */
@Slf4j
@Component
public class FlinkLargeOrderDetector {

    // 动态阈值参数
    private static final int RECENT_TRADES_SIZE = 2000;
    private static final double STANDARD_DEVIATION_MULTIPLE = 2.5;
    private static final double VOLUME_WEIGHT_FACTOR = 0.7;
    private static final double PRICE_CHANGE_WEIGHT_FACTOR = 0.3;
    private static final String KAFKA_TOPIC = "level2_trades";
    private static final String CONSUMER_GROUP_ID = "large_order_detector";
    
    // 时间衰减参数
    private static final double TIME_DECAY_LAMBDA = 0.001; // 衰减系数
    private static final long TIME_DECAY_BASE_UNIT = 60000L; // 时间衰减基准单位（毫秒）
    private static final int MIN_VALID_TRADES_FOR_THRESHOLD = 50; // 计算阈值所需的最小有效交易数
    
    // 机构大买相关参数
    private static final int INSTITUTION_BUY_THRESHOLD_COUNT = 3; // 三家机构大买阈值
    private static final long INSTITUTION_MIN_BUY_AMOUNT = 100000000; // 机构最小买入金额（分）
    private static final double INSTITUTION_BUY_RATIO_THRESHOLD = 0.3; // 机构买入占比阈值
    
    // 机构席位标识
    private static final String INSTITUTION_SEAT_FLAG = "机构专用";
    
    // 机构席位名称集合（静态常量，提高查找效率）
    private static final java.util.Set<String> INSTITUTION_SEAT_NAMES = java.util.Collections.unmodifiableSet(new java.util.HashSet<>(java.util.Arrays.asList(
        "机构专用", "机构买入专用", "机构卖出专用", "QFII专用", 
        "社保基金", "保险公司", "养老金", "证券投资基金",
        "资产管理计划", "企业年金", "信托计划", "证券公司自营"
    )));
    
    // 常见游资席位名称集合（静态常量，提高查找效率）
    private static final java.util.Set<String> HOT_MONEY_SEATS = java.util.Collections.unmodifiableSet(new java.util.HashSet<>(java.util.Arrays.asList(
        "朱雀大街", "南京唯宁路", "华泰证券深圳益田路", "国盛证券宁波桑田路",
        "国泰君安上海江苏路", "东方证券上海浦东新区银城中路", "银河证券绍兴",
        "中信证券上海溧阳路", "光大证券宁波解放南路", "方正证券杭州延安路"
    )));

    @Autowired
    private StreamExecutionEnvironment env;

    /**
     * 用于测试的大单检测方法
     */
    public LargeOrderResult detectLargeOrders(RealTimeQuote quote) {
        long startTime = System.currentTimeMillis();
        
        // 模拟大单检测逻辑
        List<LargeOrderEvent> largeOrders = new ArrayList<>();
        
        // 假设成交额超过100万为大单
        long threshold = 100_000_000L;
        
        if (quote != null && quote.getAmount() > threshold) {
            // 创建模拟的大单事件
            LargeOrderEvent orderEvent = new LargeOrderEvent(
                this,
                quote.getStockCode(),
                quote.getAmount(),
                quote.getCurrentPrice(),
                "BUY",
                "吸筹"
            );
            largeOrders.add(orderEvent);
        }
        
        long endTime = System.currentTimeMillis();
        return new LargeOrderResult(
            quote != null ? quote.getStockCode() : "",
            largeOrders,
            endTime - startTime,
            threshold
        );
    }

    /**
     * 启动 Flink 作业
     */
    public void startFlinkJob() {
        try {
            // 1. 设置 Kafka 消费者配置
            Properties properties = new Properties();
            properties.setProperty("bootstrap.servers", "localhost:9092");
            properties.setProperty("group.id", CONSUMER_GROUP_ID);
            properties.setProperty("auto.offset.reset", "latest");
            properties.setProperty("enable.auto.commit", "true");
            properties.setProperty("auto.commit.interval.ms", "1000");

            // 2. 创建 Kafka 数据源
            DataStream<String> kafkaStream = env
                    .addSource(new FlinkKafkaConsumer<>(KAFKA_TOPIC, new SimpleStringSchema(), properties));

            // 3. 解析 JSON 数据为 TradeData 对象
            DataStream<TradeData> tradeStream = kafkaStream
                    .map(json -> {
                        // 实际应用中应使用 JSON 解析库解析
                        // 这里为了演示，模拟解析
                        return mockTradeDataFromJson(json);
                    })
                    .filter(trade -> trade != null);

            // 4. 按键分组（按股票代码）
            KeyedStream<TradeData, String> keyedStream = tradeStream
                    .keyBy((KeySelector<TradeData, String>) TradeData::getStockCode);

            // 5. 滑动窗口计算动态阈值
            DataStream<LargeOrderEvent> largeOrderStream = keyedStream
                    .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
                    .aggregate(new DynamicThresholdAggregator())
                    .filter(largeOrder -> largeOrder != null);

            // 6. 处理检测到的大单
            largeOrderStream.addSink(new LargeOrderSink());

            // 7. 执行 Flink 作业
            env.execute("Large Order Detector Job");

            log.info("Flink 大单检测作业已启动");
        } catch (Exception e) {
            log.error("启动 Flink 作业失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 动态阈值聚合函数
     */
    private static class DynamicThresholdAggregator implements AggregateFunction<TradeData, Tuple2<TradeData[], Integer>, LargeOrderEvent> {

        @Override
        public Tuple2<TradeData[], Integer> createAccumulator() {
            return Tuple2.of(new TradeData[RECENT_TRADES_SIZE], 0);
        }

        @Override
        public Tuple2<TradeData[], Integer> add(TradeData trade, Tuple2<TradeData[], Integer> accumulator) {
            // 更新近期交易数据（环形数组）
            int index = accumulator.f1 % RECENT_TRADES_SIZE;
            accumulator.f0[index] = trade;
            accumulator.f1++;
            return accumulator;
        }

        @Override
        public LargeOrderEvent getResult(Tuple2<TradeData[], Integer> accumulator) {
            // 获取当前交易数据
            int count = accumulator.f1;
            TradeData[] recentTrades = accumulator.f0;

            // 检查最近一笔交易
            if (count > 0) {
                int currentIndex = (count - 1) % RECENT_TRADES_SIZE;
                TradeData currentTrade = recentTrades[currentIndex];
                
                if (currentTrade != null) {
                    // 计算动态阈值
                    long threshold = calculateDynamicThreshold(accumulator);

                    if (currentTrade.getAmount() > threshold) {
                        // 计算价格趋势
                        double priceTrend = calculatePriceTrend(recentTrades, count);
                        
                        // 检测是否存在三家机构大买
                        boolean threeInstitutionsBuy = detectThreeInstitutionsBuy(recentTrades, count);
                        
                        // 识别意图 - 使用更新后的方法
                        String intent = identifyIntent(currentTrade.getAmount(), threshold, 
                            currentTrade.getDirection(), currentTrade.getPrice(), priceTrend,
                            currentTrade, recentTrades);
                        
                        // 如果是三家机构大买，调整意图
                        if (threeInstitutionsBuy) {
                            intent = "三家机构大买";
                        }

                        // 创建大单事件
                        return new LargeOrderEvent(
                                DynamicThresholdAggregator.class,
                                currentTrade.getStockCode(),
                                currentTrade.getAmount(),
                                currentTrade.getPrice(),
                                currentTrade.getDirection(),
                                intent
                        );
                    }
                }
            }

            return null;
        }
        
        /**
         * 检测是否存在三家机构大买
         * 条件：
         * 1. 最近1小时内至少有3家不同的机构席位买入
         * 2. 每家机构买入金额都超过机构最小买入金额
         * 3. 机构买入总额占比超过阈值
         * 4. 使用时间衰减因子，最近的交易权重更高
         */
        private boolean detectThreeInstitutionsBuy(TradeData[] recentTrades, int count) {
            // 时间窗口：最近1小时（3600000毫秒）
            long timeWindowMs = 3600000L;
            long currentTime = System.currentTimeMillis();
            
            // 统计机构买入信息
            java.util.Set<String> institutionSeats = new java.util.HashSet<>();
            long totalInstitutionBuyAmount = 0;
            long totalTradeAmount = 0;
            
            // 只遍历最近的500笔交易，避免不必要的计算
            int maxTradeCount = Math.min(count, RECENT_TRADES_SIZE);
            int checkCount = Math.min(500, maxTradeCount);
            
            for (int i = 0; i < checkCount; i++) {
                int index = (count - 1 - i) % RECENT_TRADES_SIZE;
                if (index < 0) {
                    index += RECENT_TRADES_SIZE;
                }
                
                TradeData trade = recentTrades[index];
                if (trade != null) {
                    try {
                        // 优化：使用更高效的时间解析方式
                        // 假设tradeTime格式为yyyy-MM-ddTHH:mm:ss
                        String timeStr = trade.getTradeTime();
                        long tradeTimeMs = parseTimeStringToMs(timeStr);
                        
                        // 检查是否在时间窗口内
                        if (currentTime - tradeTimeMs > timeWindowMs) {
                            continue;
                        }
                        
                        // 计算时间衰减因子（使用快速近似）
                        long timeDiff = currentTime - tradeTimeMs;
                        double timeDecayFactor = calculateTimeDecayFactor(timeDiff);
                        
                        // 计算加权交易金额
                        long weightedAmount = (long) (trade.getAmount() * timeDecayFactor);
                        totalTradeAmount += weightedAmount;
                        
                        // 只统计买入方向的机构交易
                        if ("BUY".equals(trade.getDirection()) && 
                            FlinkLargeOrderDetector.isInstitutionSeat(trade) &&
                            trade.getAmount() >= INSTITUTION_MIN_BUY_AMOUNT) {

                            if (trade.getSeatName() != null) {
                                institutionSeats.add(trade.getSeatName());
                            }
                            totalInstitutionBuyAmount += weightedAmount;
                        }
                    } catch (Exception e) {
                        // 忽略时间解析错误
                        continue;
                    }
                }
            }
            
            // 检查条件
            boolean hasThreeInstitutions = institutionSeats.size() >= INSTITUTION_BUY_THRESHOLD_COUNT;
            double institutionBuyRatio = totalTradeAmount > 0 ? (double) totalInstitutionBuyAmount / totalTradeAmount : 0;
            boolean meetsBuyRatio = institutionBuyRatio >= INSTITUTION_BUY_RATIO_THRESHOLD;
            
            return hasThreeInstitutions && meetsBuyRatio;
        }

        @Override
        public Tuple2<TradeData[], Integer> merge(Tuple2<TradeData[], Integer> a, Tuple2<TradeData[], Integer> b) {
            // 合并逻辑（窗口合并时使用）
            TradeData[] mergedData = new TradeData[RECENT_TRADES_SIZE];
            int totalCount = a.f1 + b.f1;
            
            // 合并数据
            for (int i = 0; i < RECENT_TRADES_SIZE; i++) {
                if (i < a.f1 && a.f0[i % RECENT_TRADES_SIZE] != null) {
                    mergedData[i] = a.f0[i % RECENT_TRADES_SIZE];
                } else if (i >= a.f1 && i - a.f1 < b.f1 && b.f0[(i - a.f1) % RECENT_TRADES_SIZE] != null) {
                    mergedData[i] = b.f0[(i - a.f1) % RECENT_TRADES_SIZE];
                }
            }
            
            return Tuple2.of(mergedData, totalCount);
        }

        /**
         * 计算动态阈值 - 使用均值+N倍标准差的方法，并加入更多市场环境因子
         */
        private long calculateDynamicThreshold(Tuple2<TradeData[], Integer> accumulator) {
            TradeData[] data = accumulator.f0;
            int count = accumulator.f1;
            
            if (count < MIN_VALID_TRADES_FOR_THRESHOLD) {
                return 50_000_000L; // 默认50万
            }
            
            // 获取当前时间用于计算时间差
            LocalDateTime currentTime = LocalDateTime.now();
            
            // 计算带时间衰减的交易金额和市场活跃度
            long[] weightedAmounts = new long[Math.min(count, RECENT_TRADES_SIZE)];
            int[] prices = new int[Math.min(count, RECENT_TRADES_SIZE)];
            long[] volumes = new long[Math.min(count, RECENT_TRADES_SIZE)];
            int validCount = 0;
            
            // 收集有效数据并计算带时间衰减的市场活跃度
            int recentTradeCount = 0;
            long recentTradeVolume = 0;
            long recentWeightedVolume = 0;
            int maxTimeWindow = Math.min(RECENT_TRADES_SIZE, count);
            int recentWindow = Math.min(50, maxTimeWindow);
            
            // 新增：计算盘口买卖盘比例
            int buyOrderCount = 0;
            int sellOrderCount = 0;
            long totalBuyAmount = 0;
            long totalSellAmount = 0;
            
            for (int i = 0; i < maxTimeWindow; i++) {
                if (data[i] != null) {
                    TradeData trade = data[i];
                    
                    // 计算时间差（毫秒）
                    long timeDiff = calculateTimeDiff(currentTime, trade.getTradeTime());
                    
                    // 计算时间衰减因子
                    double timeDecayFactor = calculateTimeDecayFactor(timeDiff);
                    
                    // 计算加权交易金额
                    long weightedAmount = (long) (trade.getAmount() * timeDecayFactor);
                    weightedAmounts[validCount] = weightedAmount;
                    prices[validCount] = trade.getPrice();
                    volumes[validCount] = trade.getAmount();
                    validCount++;
                    
                    // 计算近期活跃度（最近50笔交易）
                    if (i >= maxTimeWindow - recentWindow) {
                        recentTradeCount++;
                        recentTradeVolume += trade.getAmount();
                        recentWeightedVolume += weightedAmount;
                    }
                    
                    // 统计买卖盘数据
                    if ("BUY".equals(trade.getDirection())) {
                        buyOrderCount++;
                        totalBuyAmount += weightedAmount;
                    } else if ("SELL".equals(trade.getDirection())) {
                        sellOrderCount++;
                        totalSellAmount += weightedAmount;
                    }
                }
            }
            
            if (validCount == 0) {
                return 50_000_000L;
            }
            
            // 调整数组大小
            if (validCount < weightedAmounts.length) {
                long[] tempAmounts = new long[validCount];
                int[] tempPrices = new int[validCount];
                long[] tempVolumes = new long[validCount];
                System.arraycopy(weightedAmounts, 0, tempAmounts, 0, validCount);
                System.arraycopy(prices, 0, tempPrices, 0, validCount);
                System.arraycopy(volumes, 0, tempVolumes, 0, validCount);
                weightedAmounts = tempAmounts;
                prices = tempPrices;
                volumes = tempVolumes;
            }
            
            // 计算均值
            long totalWeightedVolume = 0;
            for (long amount : weightedAmounts) {
                totalWeightedVolume += amount;
            }
            long mean = totalWeightedVolume / validCount;
            
            // 计算标准差
            double sumSquaredDiff = 0;
            for (long amount : weightedAmounts) {
                double diff = amount - mean;
                sumSquaredDiff += diff * diff;
            }
            double variance = sumSquaredDiff / validCount;
            double standardDeviation = Math.sqrt(variance);
            
            // 计算市场活跃度因子（带时间衰减，考虑成交量变化率）
            double activityFactor = 1.0;
            if (recentTradeCount > 0) {
                long avgRecentVolume = recentTradeVolume / recentTradeCount;
                long avgRecentWeightedVolume = recentWeightedVolume / recentTradeCount;
                
                // 计算整体平均加权交易量
                long avgOverallWeightedVolume = totalWeightedVolume / validCount;
                
                // 计算成交量变化率
                double volumeChangeRate = 1.0;
                if (avgOverallWeightedVolume > 0) {
                    volumeChangeRate = avgRecentWeightedVolume / (double) avgOverallWeightedVolume;
                }
                
                // 计算成交量波动率
                double volumeVolatility = calculateVolumeVolatility(weightedAmounts, avgOverallWeightedVolume);
                
                // 新增：计算价格波动率
                double priceVolatility = calculatePriceVolatility(prices);
                
                // 新增：计算盘口买卖盘比例
                double orderBalanceRatio = calculateOrderBalanceRatio(buyOrderCount, sellOrderCount);
                
                // 新增：计算成交笔数变化率
                double tradeCountChangeRate = calculateTradeCountChangeRate(maxTimeWindow, recentWindow);
                
                // 新增：计算价格位置因子（相对最近价格范围的位置）
                double pricePositionFactor = calculatePricePositionFactor(prices);
                
                // 综合计算活跃度因子
                // 基础活跃度：成交量变化率
                double baseActivity = volumeChangeRate;
                
                // 调整因子：成交量波动率（波动率越高，活跃度越高）
                double volatilityFactor = 1.0 + volumeVolatility;
                
                // 调整因子：价格波动率
                double priceVolatilityFactor = 1.0 + priceVolatility;
                
                // 调整因子：盘口买卖盘比例（极端的买卖盘比例会影响活跃度）
                double balanceFactor = 1.0 + Math.abs(orderBalanceRatio - 0.5);
                
                // 调整因子：成交笔数变化率
                double tradeCountFactor = 1.0 + (tradeCountChangeRate - 1.0);
                
                // 调整因子：价格位置因子
                double priceFactor = 1.0 + pricePositionFactor;
                
                // 综合计算活跃度因子
                activityFactor = baseActivity * volatilityFactor * priceVolatilityFactor * 
                                balanceFactor * tradeCountFactor * priceFactor;
                
                // 限制活跃度因子的范围
                activityFactor = Math.max(0.5, Math.min(3.0, activityFactor));
            }
            
            // 计算最终阈值：均值 + N倍标准差 + 市场活跃度因子
            long baseThreshold = Math.round(mean + STANDARD_DEVIATION_MULTIPLE * standardDeviation);
            long finalThreshold = Math.round(baseThreshold * activityFactor);
            
            // 设置最小阈值，避免过低
            return Math.max(finalThreshold, 30_000_000L);
        }
        
        /**
         * 计算成交量波动率
         */
        private double calculateVolumeVolatility(long[] weightedAmounts, long avgVolume) {
            if (avgVolume == 0) {
                return 0.0;
            }
            
            double sum = 0;
            int count = weightedAmounts.length;
            
            for (long volume : weightedAmounts) {
                double ratio = volume / (double) avgVolume;
                sum += Math.abs(ratio - 1.0);
            }
            
            return sum / count;
        }
        
        /**
         * 计算价格波动率
         */
        private double calculatePriceVolatility(int[] prices) {
            if (prices.length < 2) {
                return 0.0;
            }
            
            double sum = 0;
            int count = prices.length;
            
            // 计算平均价格
            long totalPrice = 0;
            for (int price : prices) {
                totalPrice += price;
            }
            double avgPrice = totalPrice / (double) count;
            
            if (avgPrice == 0) {
                return 0.0;
            }
            
            // 计算价格波动百分比
            for (int price : prices) {
                double ratio = price / avgPrice;
                sum += Math.abs(ratio - 1.0);
            }
            
            return sum / count;
        }
        
        /**
         * 计算盘口买卖盘比例
         */
        private double calculateOrderBalanceRatio(int buyOrderCount, int sellOrderCount) {
            if (buyOrderCount + sellOrderCount == 0) {
                return 0.5;
            }
            
            return buyOrderCount / (double) (buyOrderCount + sellOrderCount);
        }
        
        /**
         * 计算成交笔数变化率
         */
        private double calculateTradeCountChangeRate(int maxTimeWindow, int recentWindow) {
            if (maxTimeWindow == 0 || recentWindow == 0) {
                return 1.0;
            }
            
            // 计算近期成交笔数占比
            double recentRatio = recentWindow / (double) maxTimeWindow;
            
            // 计算变化率（基础值为1）
            return 1.0 + (recentRatio - (recentWindow / RECENT_TRADES_SIZE));
        }
        
        /**
         * 计算价格位置因子（相对最近价格范围的位置）
         */
        private double calculatePricePositionFactor(int[] prices) {
            if (prices.length < 2) {
                return 0.0;
            }
            
            int maxPrice = Integer.MIN_VALUE;
            int minPrice = Integer.MAX_VALUE;
            
            // 找到最近价格范围
            for (int price : prices) {
                maxPrice = Math.max(maxPrice, price);
                minPrice = Math.min(minPrice, price);
            }
            
            if (maxPrice == minPrice) {
                return 0.0;
            }
            
            // 计算当前价格（假设是最后一个价格）
            int currentPrice = prices[prices.length - 1];
            
            // 计算相对位置
            double position = (currentPrice - minPrice) / (double) (maxPrice - minPrice);
            
            // 标准化为-0.5到0.5之间的因子
            return position - 0.5;
        }
        
        /**
         * 计算时间差（毫秒）
         */
        private long calculateTimeDiff(LocalDateTime currentTime, String tradeTimeStr) {
            try {
                // 假设tradeTimeStr格式为yyyy-MM-ddTHH:mm:ss
                LocalDateTime tradeTime = LocalDateTime.parse(tradeTimeStr);
                return java.time.Duration.between(tradeTime, currentTime).toMillis();
            } catch (Exception e) {
                return 0;
            }
        }
        
        /**
         * 解析时间字符串为毫秒
         */
        private long parseTimeStringToMs(String timeStr) {
            try {
                // 假设timeStr格式为yyyy-MM-ddTHH:mm:ss
                LocalDateTime tradeTime = LocalDateTime.parse(timeStr);
                return tradeTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception e) {
                return System.currentTimeMillis();
            }
        }
    }
    
    /**
     * 计算价格趋势 - 使用时间衰减的加权移动平均
     */
    private static double calculatePriceTrend(TradeData[] recentTrades, int count) {
        if (count < 10) {
            return 0.0; // 数据不足时返回平稳趋势
        }
        
        double weightedSum = 0.0;
        double weightSum = 0.0;
        
        // 只考虑最近的100笔交易
        int windowSize = Math.min(100, count);
        
        for (int i = 0; i < windowSize; i++) {
            int index = (count - 1 - i) % RECENT_TRADES_SIZE;
            if (index < 0) {
                index += RECENT_TRADES_SIZE;
            }
            
            TradeData trade = recentTrades[index];
            if (trade != null) {
                // 时间衰减因子：越近的交易权重越高
                double weight = Math.exp(-TIME_DECAY_LAMBDA * i);
                
                // 计算价格变化百分比
                double priceChangePercent = 0.0;
                if (i > 0) {
                    int prevIndex = (count - 1 - (i - 1)) % RECENT_TRADES_SIZE;
                    if (prevIndex < 0) {
                        prevIndex += RECENT_TRADES_SIZE;
                    }
                    TradeData prevTrade = recentTrades[prevIndex];
                    if (prevTrade != null && prevTrade.getPrice() != 0) {
                        priceChangePercent = (trade.getPrice() - prevTrade.getPrice()) / (double) prevTrade.getPrice();
                    }
                }
                
                weightedSum += priceChangePercent * weight;
                weightSum += weight;
            }
        }
        
        if (weightSum == 0) {
            return 0.0;
        }
        
        // 返回价格趋势（-1.0到1.0之间）
        double trend = weightedSum / weightSum;
        return Math.max(-1.0, Math.min(1.0, trend));
    }
    
    /**
     * 识别交易意图 - 使用改进的逻辑
     */
    public static String identifyIntent(long tradeAmount, long threshold, 
                                        String direction, int currentPrice, double priceTrend,
                                        TradeData currentTrade, TradeData[] recentTrades) {
        
        // 基础意图识别
        String baseIntent;
        if (direction.equals("BUY")) {
            baseIntent = "主力吸筹";
        } else {
            baseIntent = "主力出货";
        }
        
        // 计算超额比例
        double excessRatio = tradeAmount / (double) threshold;
        
        // 识别特殊意图
        if (excessRatio >= 2.0) {
            if (direction.equals("BUY")) {
                return "主力大量吸筹";
            } else {
                return "主力大量出货";
            }
        } else if (excessRatio >= 1.5) {
            if (direction.equals("BUY")) {
                return "主力积极吸筹";
            } else {
                return "主力积极出货";
            }
        }
        
        // 考虑价格趋势
        if (direction.equals("BUY") && priceTrend > 0.05) {
            return "主力拉升吸筹";
        } else if (direction.equals("BUY") && priceTrend < -0.05) {
            return "主力逆势吸筹";
        } else if (direction.equals("SELL") && priceTrend > 0.05) {
            return "主力高位出货";
        } else if (direction.equals("SELL") && priceTrend < -0.05) {
            return "主力砸盘出货";
        }
        
        // 考虑机构席位
        if (isInstitutionSeat(currentTrade)) {
            if (direction.equals("BUY")) {
                return "机构吸筹";
            } else {
                return "机构出货";
            }
        }
        
        // 考虑游资席位
        if (isHotMoneySeat(currentTrade)) {
            if (direction.equals("BUY")) {
                return "游资吸筹";
            } else {
                return "游资出货";
            }
        }
        
        // 默认返回基础意图
        return baseIntent;
    }
    
    /**
     * 检测是否为机构席位
     */
    private static boolean isInstitutionSeat(TradeData trade) {
        if (trade.getSeatName() == null) {
            return false;
        }
        
        // 检查是否包含机构专用标识
        if (trade.getSeatName().contains(INSTITUTION_SEAT_FLAG)) {
            return true;
        }
        
        // 检查是否在机构席位列表中
        return INSTITUTION_SEAT_NAMES.contains(trade.getSeatName());
    }
    
    /**
     * 检测是否为游资席位
     */
    private static boolean isHotMoneySeat(TradeData trade) {
        if (trade.getSeatName() == null) {
            return false;
        }
        
        // 检查是否在游资席位列表中
        return HOT_MONEY_SEATS.contains(trade.getSeatName());
    }
    
    /**
     * 计算时间衰减因子 - 使用指数衰减
     */
    private static double calculateTimeDecayFactor(long timeDiff) {
        return Math.exp(-TIME_DECAY_LAMBDA * (timeDiff / TIME_DECAY_BASE_UNIT));
    }
    
    /**
     * 模拟从 JSON 解析 TradeData 对象
     */
    private TradeData mockTradeDataFromJson(String json) {
        // 简单实现：从 JSON 字符串中提取关键信息
        try {
            // 实际应用中应使用 JSON 解析库
            // 这里模拟解析，返回一个示例 TradeData 对象
            String stockCode = extractValue(json, "stockCode");
            long amount = Long.parseLong(extractValue(json, "amount"));
            int price = Integer.parseInt(extractValue(json, "price"));
            String direction = extractValue(json, "direction");
            String tradeTime = extractValue(json, "tradeTime");
            String seatName = extractValue(json, "seatName");
            
            return TradeData.builder()
                    .stockCode(stockCode)
                    .amount(amount)
                    .price(price)
                    .direction(direction)
                    .tradeTime(tradeTime)
                    .seatName(seatName)
                    .build();
        } catch (Exception e) {
            log.error("解析 JSON 失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从 JSON 字符串中提取值（简化实现）
     */
    private String extractValue(String json, String key) {
        // 简单实现：查找 key 和对应的值
        int keyIndex = json.indexOf('"' + key + '"');
        if (keyIndex == -1) {
            return null;
        }
        
        int colonIndex = json.indexOf(':', keyIndex);
        if (colonIndex == -1) {
            return null;
        }
        
        int startIndex = json.indexOf('"', colonIndex + 1);
        if (startIndex == -1) {
            return null;
        }
        
        int endIndex = json.indexOf('"', startIndex + 1);
        if (endIndex == -1) {
            return null;
        }
        
        return json.substring(startIndex + 1, endIndex);
    }
    
    /**
     * 大单事件处理器
     */
    private static class LargeOrderSink implements org.apache.flink.streaming.api.functions.sink.SinkFunction<LargeOrderEvent> {
        @Override
        public void invoke(LargeOrderEvent value, Context context) {
            // 处理检测到的大单事件
            log.info("检测到大单: 股票代码={}, 金额={}, 价格={}, 方向={}, 意图={}, 时间={}",
                    value.getStockCode(),
                    value.getAmount(),
                    value.getPrice(),
                    value.getDirection(),
                    value.getIntent(),
                    value.getTradeTime());
            
            // 可以在这里添加更多处理逻辑，如发送通知、存储数据库等
        }
    }
}