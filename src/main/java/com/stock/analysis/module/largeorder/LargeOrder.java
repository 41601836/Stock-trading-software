package com.stock.analysis.module.largeorder;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 大单异动实体类
 * 对应表: t_large_order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LargeOrder implements Serializable {

    private Long id;

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 交易时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tradeTime;

    /**
     * 成交价格 (单位: 分)
     */
    private Integer price;

    /**
     * 成交量 (股)
     */
    private Long volume;

    /**
     * 成交金额 (单位: 分)
     */
    private Long amount;

    /**
     * 方向: BUY/SELL/NEUTRAL
     */
    private String direction;

    /**
     * 异动类型: ROCKET_LAUNCH(直线拉升), LARGE_BUY(大单买入)等
     */
    private String orderType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
