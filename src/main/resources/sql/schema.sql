-- ----------------------------
-- 1. 筹码分布表 (StockChip)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_stock_chip` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stock_code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `analyze_time` DATETIME NOT NULL COMMENT '分析时间',
    `current_price` INT NOT NULL COMMENT '当前价格(分)',
    `main_cost` INT NOT NULL COMMENT '主筹成本(分)',
    `price_range_low` INT NOT NULL COMMENT '核心筹码区间下限(分)',
    `price_range_high` INT NOT NULL COMMENT '核心筹码区间上限(分)',
    `chip_ratio` DECIMAL(10, 4) NOT NULL COMMENT '区间筹码占比(如 0.8500 表示 85%)',
    `concentration_ratio` DECIMAL(10, 4) COMMENT '90%筹码集中度',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_stock_time` (`stock_code`, `analyze_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='筹码分布分析表';

-- ----------------------------
-- 2. 大单异动表 (LargeOrder)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_large_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stock_code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `trade_time` DATETIME NOT NULL COMMENT '交易时间',
    `price` INT NOT NULL COMMENT '成交价格(分)',
    `volume` BIGINT NOT NULL COMMENT '成交量(股)',
    `amount` BIGINT NOT NULL COMMENT '成交金额(分)',
    `direction` VARCHAR(10) NOT NULL COMMENT '方向: BUY/SELL/NEUTRAL',
    `order_type` VARCHAR(32) NOT NULL COMMENT '异动类型: ROCKET_LAUNCH(直线拉升), LARGE_BUY(大单买入)等',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_stock_time` (`stock_code`, `trade_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大单异动记录表';

-- ----------------------------
-- 3. 舆情数据表 (PublicOpinion)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_public_opinion` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stock_code` VARCHAR(10) COMMENT '关联股票代码(可为空)',
    `publish_time` DATETIME NOT NULL COMMENT '发布时间',
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容摘要',
    `source` VARCHAR(50) NOT NULL COMMENT '来源: 财联社/雪球/东方财富等',
    `sentiment_score` INT NOT NULL DEFAULT 50 COMMENT '情感评分(0-100, 50中性)',
    `tags` VARCHAR(255) COMMENT '事件标签(逗号分隔)',
    `url` VARCHAR(512) COMMENT '原文链接',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`),
    INDEX `idx_publish_time` (`publish_time`),
    INDEX `idx_stock` (`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='舆情分析数据表';
