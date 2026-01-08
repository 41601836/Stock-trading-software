-- ----------------------------
-- 1. 筹码分布表 (StockChip)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_stock_chip` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stock_code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `analyze_time` TIMESTAMP NOT NULL COMMENT '分析时间',
    `current_price` INT NOT NULL COMMENT '当前价格(分)',
    `main_cost` INT NOT NULL COMMENT '主筹成本(分)',
    `price_range_low` INT NOT NULL COMMENT '核心筹码区间下限(分)',
    `price_range_high` INT NOT NULL COMMENT '核心筹码区间上限(分)',
    `chip_ratio` DECIMAL(10, 4) NOT NULL COMMENT '区间筹码占比(如 0.8500 表示 85%)',
    `concentration_ratio` DECIMAL(10, 4) COMMENT '90%筹码集中度',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_stock_time` (`stock_code`, `analyze_time`)
) COMMENT='筹码分布分析表';

-- ----------------------------
-- 2. 大单异动表 (LargeOrder)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_large_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stock_code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `trade_time` TIMESTAMP NOT NULL COMMENT '交易时间',
    `price` INT NOT NULL COMMENT '成交价格(分)',
    `volume` BIGINT NOT NULL COMMENT '成交量(股)',
    `amount` BIGINT NOT NULL COMMENT '成交金额(分)',
    `direction` VARCHAR(10) NOT NULL COMMENT '方向: BUY/SELL/NEUTRAL',
    `order_type` VARCHAR(32) NOT NULL COMMENT '异动类型: ROCKET_LAUNCH(直线拉升), LARGE_BUY(大单买入)等',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_large_order_stock_time` (`stock_code`, `trade_time`)
) COMMENT='大单异动记录表';

-- ----------------------------
-- 3. 舆情数据表 (PublicOpinion)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_public_opinion` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stock_code` VARCHAR(10) COMMENT '关联股票代码(可为空)',
    `publish_time` TIMESTAMP NOT NULL COMMENT '发布时间',
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容摘要',
    `source` VARCHAR(50) NOT NULL COMMENT '来源: 财联社/雪球/东方财富等',
    `sentiment_score` INT NOT NULL DEFAULT 50 COMMENT '情感评分(0-100, 50中性)',
    `tags` VARCHAR(255) COMMENT '事件标签(逗号分隔)',
    `url` VARCHAR(512) COMMENT '原文链接',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`),
    INDEX `idx_publish_time` (`publish_time`),
    INDEX `idx_stock` (`stock_code`)
) COMMENT='舆情分析数据表';

-- ----------------------------
-- 4. K线数据表 (Kline)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_kline` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stock_code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `date` DATE NOT NULL COMMENT '日期',
    `open` INT NOT NULL COMMENT '开盘价(分)',
    `close` INT NOT NULL COMMENT '收盘价(分)',
    `high` INT NOT NULL COMMENT '最高价(分)',
    `low` INT NOT NULL COMMENT '最低价(分)',
    `volume` BIGINT NOT NULL COMMENT '成交量(股)',
    `amount` BIGINT NOT NULL COMMENT '成交金额(分)',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_date` (`stock_code`, `date`),
    INDEX `idx_date` (`date`)
) COMMENT='K线数据表';

-- ----------------------------
-- 5. 持仓表 (Position)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_position` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stock_code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `stock_name` VARCHAR(50) NOT NULL COMMENT '股票名称',
    `cost_price` INT NOT NULL COMMENT '成本价(分)',
    `current_price` INT NOT NULL COMMENT '现价(分)',
    `highest_price` INT NOT NULL COMMENT '持仓期间最高价(分)',
    `quantity` INT NOT NULL COMMENT '持仓数量(股)',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock` (`stock_code`)
) COMMENT='持仓表';

-- ----------------------------
-- 6. 风险评估表 (RiskAssessment)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_risk_assessment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `total_asset` BIGINT NOT NULL COMMENT '账户总资产(分)',
    `value_at_risk` BIGINT NOT NULL COMMENT 'VaR值(分)',
    `health_score` INT NOT NULL COMMENT '账户健康分(0-100)',
    `risk_warnings` VARCHAR(512) COMMENT '风险警告(逗号分隔)',
    `suggestion` VARCHAR(255) COMMENT '建议操作',
    `assessment_time` TIMESTAMP NOT NULL COMMENT '评估时间',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_assessment_time` (`assessment_time`)
) COMMENT='风险评估表';

-- ----------------------------
-- 7. 舆情汇总表 (OpinionSummary)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_opinion_summary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stock_code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `summary_date` DATE NOT NULL COMMENT '汇总日期',
    `positive_count` INT NOT NULL DEFAULT 0 COMMENT '正面舆情数量',
    `negative_count` INT NOT NULL DEFAULT 0 COMMENT '负面舆情数量',
    `neutral_count` INT NOT NULL DEFAULT 0 COMMENT '中性舆情数量',
    `avg_sentiment_score` DECIMAL(5, 2) NOT NULL DEFAULT 50.00 COMMENT '平均情感评分',
    `hot_topics` VARCHAR(512) COMMENT '热门话题(逗号分隔)',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_opinion_stock_date` (`stock_code`, `summary_date`)
) COMMENT='舆情汇总表';
