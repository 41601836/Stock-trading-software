package com.stock.analysis.module.heatflow;

import com.stock.analysis.common.model.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 席位溢价率模型
 */
@Data
@Accessors(chain = true)
public class SeatPremiumRate extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 席位代码
     */
    private String seatCode;
    
    /**
     * 席位名称
     */
    private String seatName;
    
    /**
     * 近30天次日平均涨幅（‰）
     */
    private Integer avgNextDayIncrease;
    
    /**
     * 近30天5日平均涨幅（‰）
     */
    private Integer avgFiveDayIncrease;
    
    /**
     * 上涨概率（%）
     */
    private Integer winRate;
    
    /**
     * 统计天数
     */
    private Integer statDays;
}