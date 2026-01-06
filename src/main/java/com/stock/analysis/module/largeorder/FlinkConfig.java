package com.stock.analysis.module.largeorder;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flink 配置类
 */
@Configuration
public class FlinkConfig {

    @Bean
    public StreamExecutionEnvironment streamExecutionEnvironment() {
        // 创建流处理环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // 设置并行度
        env.setParallelism(4);
        
        // 设置时间特性为事件时间
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        
        // 设置重启策略：固定延迟，3次尝试，每次间隔10秒
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, 10000));
        
        // 启用检查点，间隔1分钟
        env.enableCheckpointing(60000);
        
        return env;
    }

    @Bean
    public StreamTableEnvironment streamTableEnvironment(StreamExecutionEnvironment env) {
        return StreamTableEnvironment.create(env);
    }
}
