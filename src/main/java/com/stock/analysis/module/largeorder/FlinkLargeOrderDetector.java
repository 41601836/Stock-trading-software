package com.stock.analysis.module.largeorder;

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

    @Autowired
    private StreamExecutionEnvironment env;

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
                    .keyBy((KeySelector<TradeData, String>) TradeData::stockCode);

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
                        
                        // 识别意图
                        String intent = identifyIntent(currentTrade.getAmount(), threshold, 
                            currentTrade.getDirection(), currentTrade.getPrice(), priceTrend);

                        // 创建大单事件
                        return new LargeOrderEvent(
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
         * 计算动态阈值
         */
        private long calculateDynamicThreshold(Tuple2<TradeData[], Integer> accumulator) {
            TradeData[] data = accumulator.f0;
            int count = accumulator.f1;
            
            if (count < 50) {
                return 50_000_000L; // 默认50万
            }
            
            // 计算均值和标准差
            double mean = 0;
            int validCount = 0;
            
            for (int i = 0; i < Math.min(count, RECENT_TRADES_SIZE); i++) {
                if (data[i] != null) {
                    mean += data[i].getAmount();
                    validCount++;
                }
            }
            
            if (validCount == 0) {
                return 50_000_000L;
            }
            
            mean /= validCount;
            
            // 计算标准差
            double variance = 0;
            for (int i = 0; i < Math.min(count, RECENT_TRADES_SIZE); i++) {
                if (data[i] != null) {
                    variance += Math.pow(data[i].getAmount() - mean, 2);
                }
            }
            variance /= validCount;
            double stdDev = Math.sqrt(variance);
            
            // 动态阈值 = 均值 + N倍标准差
            long threshold = (long)(mean + STANDARD_DEVIATION_MULTIPLE * stdDev);
            
            // 确保阈值不小于最小阈值
            return Math.max(threshold, 20_000_000L); // 最小20万
        }
        
        /**
         * 计算价格趋势
         */
        private double calculatePriceTrend(TradeData[] recentTrades, int count) {
            if (count < 10) {
                return 0;
            }
            
            int window = Math.min(10, count);
            int prices[] = new int[window];
            int index = 0;
            
            // 获取最近window笔交易的价格
            for (int i = count - window; i < count; i++) {
                if (i >= 0) {
                    TradeData trade = recentTrades[i % RECENT_TRADES_SIZE];
                    if (trade != null) {
                        prices[index++] = trade.getPrice();
                    }
                }
            }
            
            if (index < 2) {
                return 0;
            }
            
            // 计算简单线性回归的斜率
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            for (int i = 0; i < index; i++) {
                sumX += i;
                sumY += prices[i];
                sumXY += i * prices[i];
                sumX2 += i * i;
            }
            
            double denominator = index * sumX2 - sumX * sumX;
            if (denominator == 0) {
                return 0;
            }
            
            double slope = (index * sumXY - sumX * sumY) / denominator;
            
            // 标准化斜率
            double priceRange = (double) (prices[index - 1] - prices[0]) / prices[0];
            return slope / priceRange;
        }
    }

    /**
     * 大单事件接收器
     */
    private static class LargeOrderSink implements org.apache.flink.streaming.api.functions.sink.SinkFunction<LargeOrderEvent> {
        @Override
        public void invoke(LargeOrderEvent value, Context context) {
            // 处理检测到的大单事件
            // 实际应用中可以：
            // 1. 写入数据库
            // 2. 发送通知
            // 3. 更新缓存
            log.info("检测到大单: 股票代码={}, 金额={}元, 价格={}元, 方向={}",
                    value.getStockCode(),
                    value.getAmount() / 10000.0,
                    value.getPrice() / 100.0,
                    value.getDirection());
        }
    }

    /**
     * 模拟从 JSON 解析 TradeData
     */
    private static TradeData mockTradeDataFromJson(String json) {
        // 实际应用中应使用 Jackson 或 Gson 解析
        // 这里为了演示，随机生成数据
        String stockCode = "SH600000";
        int price = 1000 + (int)(Math.random() * 200); // 10.00 - 12.00 元
        long amount = 1000000L + (long)(Math.random() * 100000000L); // 10万 - 1亿元
        String direction = Math.random() > 0.5 ? "BUY" : "SELL";
        
        return new TradeData(stockCode, price, amount, direction);
    }

    /**
     * 识别交易意图
     */
    private static String identifyIntent(long amount, long threshold, String direction, int price, double priceTrend) {
        double exceedRatio = (double) amount / threshold;
        
        if ("BUY".equals(direction)) {
            // 买入方向意图识别
            if (exceedRatio > 2.0) {
                if (priceTrend > 0.1) {
                    return "急拉吸筹";
                } else {
                    return "低位吸筹";
                }
            } else if (exceedRatio > 1.5) {
                if (priceTrend > 0) {
                    return "主动买入";
                } else {
                    return "逆势买入";
                }
            } else {
                return "买入";
            }
        } else if ("SELL".equals(direction)) {
            // 卖出方向意图识别
            if (exceedRatio > 2.0) {
                if (priceTrend < -0.1) {
                    return "砸盘出货";
                } else {
                    return "高位出货";
                }
            } else if (exceedRatio > 1.5) {
                if (priceTrend < 0) {
                    return "主动卖出";
                } else {
                    return "逆势卖出";
                }
            } else {
                return "卖出";
            }
        } else {
            return "中性";
        }
    }
}